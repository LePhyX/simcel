# SimCel — Documentation technique complète

## Table des matières

1. [Vue d'ensemble](#1-vue-densemble)
2. [Architecture MVC](#2-architecture-mvc)
3. [Modèle (`model/`)](#3-modèle-model)
   - [CellState](#31-cellstate)
   - [CellType](#32-celltype)
   - [Cell](#33-cell)
   - [WindDirection](#34-winddirection)
   - [Environment](#35-environment)
   - [Grid](#36-grid)
   - [FireSimulator](#37-firesimulator)
   - [SimulationListener](#38-simulationlistener)
   - [SimulationState](#39-simulationstate)
   - [SimulationSnapshot](#310-simulationsnapshot)
   - [SimulationIO](#311-simulationio)
4. [Contrôleur (`controller/`)](#4-contrôleur-controller)
   - [SimulationController](#41-simulationcontroller)
5. [Vue (`view/`)](#5-vue-view)
   - [GridView](#51-gridview)
   - [StatisticsPanel](#52-statisticspanel)
   - [ChartView](#53-chartview)
   - [LegendPanel](#54-legendpanel)
   - [WindIndicator](#55-windindicator)
   - [ConsoleView](#56-consoleview)
   - [MainWindow](#57-mainwindow)
6. [Point d'entrée (`Main`)](#6-point-dentrée-main)
7. [Algorithme de simulation](#7-algorithme-de-simulation)
8. [Import / Export](#8-import--export)
9. [Diagramme des dépendances](#9-diagramme-des-dépendances)
10. [Scripts de lancement](#10-scripts-de-lancement)

---

## 1. Vue d'ensemble

**SimCel** est une simulation de propagation d'incendie basée sur les **automates cellulaires**.
Le terrain est découpé en une grille 2D de cellules. À chaque pas de temps (*tick*), les cellules
en feu tentent de propager l'incendie à leurs voisines selon une probabilité stochastique qui dépend
du type de terrain, du vent et de l'humidité ambiante.

### Modes de lancement

| Mode | Activation | Description |
|------|-----------|-------------|
| **Graphique** (défaut) | `./run-gui.sh` | Interface JavaFX complète avec grille interactive, graphiques et contrôles |
| **Console** | `./run-headless.sh --headless` | Animation ANSI en terminal, paramétrable en ligne de commande |

---

## 2. Architecture MVC

Le projet suit strictement le patron **Modèle – Vue – Contrôleur** :

```
com.simcel
├── model/          ← Logique métier pure (aucune dépendance JavaFX)
│   ├── Cell, CellState, CellType
│   ├── WindDirection, Environment
│   ├── Grid, FireSimulator
│   ├── SimulationListener, SimulationState
│   ├── SimulationSnapshot, SimulationIO
├── controller/
│   └── SimulationController
├── view/           ← Composants JavaFX + ConsoleView ANSI
│   ├── GridView, StatisticsPanel, ChartView
│   ├── LegendPanel, WindIndicator
│   ├── ConsoleView, MainWindow
└── Main.java       ← Point d'entrée (JavaFX Application)
```

**Flux de données :**
```
FireSimulator.tick()
    └─► notifyListeners(tick, grid)
            ├─► GridView.onTick()       → redessine la grille (Platform.runLater)
            ├─► StatisticsPanel.onTick() → met à jour les compteurs
            └─► ChartView.onTick()      → ajoute un point aux courbes
```

---

## 3. Modèle (`model/`)

### 3.1 CellState

Énumération des 6 états possibles d'une cellule :

| Constante | Couleur | Inflammable | Description |
|-----------|---------|-------------|-------------|
| `VIDE`    | blanc   | non | Case sans végétation |
| `SAIN`    | vert    | **oui** | Végétation intacte |
| `EN_FEU`  | orange  | non | Activement en combustion |
| `BRULE`   | gris    | non | Cendres, ne peut plus brûler |
| `EAU`     | bleu    | non | Pare-feu naturel |
| `ROCHER`  | gris clair | non | Obstacle infranchissable |

Méthodes utilitaires : `isFlammable()`, `isSpreading()`, `getColor()`.

---

### 3.2 CellType

Énumération des 5 types de terrain avec leurs paramètres physiques :

| Type | Inflammabilité | Durée combustion | Couleur (SAIN) |
|------|---------------|-----------------|----------------|
| `FORET`        | 0.70 | 8 ticks  | vert foncé |
| `PRAIRIE`      | 0.90 | 3 ticks  | vert clair |
| `BROUSSAILLES` | 0.50 | 5 ticks  | brun |
| `ZONE_HUMIDE`  | 0.20 | 12 ticks | bleu-vert |
| `ZONE_URBAINE` | 0.30 | 6 ticks  | gris |

Le type est **immuable** : il ne change jamais pendant la simulation. Seul l'état évolue.

---

### 3.3 Cell

Unité élémentaire de la grille. Contient :
- `type : CellType` — immuable, détermine les propriétés physiques
- `state : CellState` — mutable, évolue à chaque tick
- `remainingBurnTime : int` — compteur décrémenté à chaque tick tant que `EN_FEU`

**Transitions métier :**
- `ignite()` — enflamme si `SAIN`, réinitialise le compteur
- `burnOut()` — passe à `BRULE`, remet le compteur à 0
- `decrementBurnTime()` — décrémente le compteur (min 0)
- `copy()` — copie profonde (utilisée pour l'historique et les snapshots)

Implémente `Serializable` pour la sauvegarde/restauration.

---

### 3.4 WindDirection

Énumération des 8 directions cardinales et inter-cardinales avec leurs vecteurs unitaires :

| Direction | dx | dy |
|-----------|----|----|
| N  |  0 | -1 |
| NE |  1 | -1 |
| E  |  1 |  0 |
| SE |  1 |  1 |
| S  |  0 |  1 |
| SO | -1 |  1 |
| O  | -1 |  0 |
| NO | -1 | -1 |

---

### 3.5 Environment

Conditions météorologiques globales influençant la propagation :

| Paramètre | Type | Plage | Description |
|-----------|------|-------|-------------|
| `direction`    | `WindDirection` | 8 valeurs | Direction du vent |
| `windStrength` | `int` | `[MIN_WIND=0, MAX_WIND=5]` | Intensité (0 = calme, 5 = tempête) |
| `humidity`     | `int` | `[MIN_HUMIDITY=0, MAX_HUMIDITY=100]` | Taux d'humidité (%) |

Les constantes `MIN_WIND`, `MAX_WIND`, `MIN_HUMIDITY`, `MAX_HUMIDITY` sont publiques et réutilisées
par `ConsoleView`, `WindIndicator` et `FireSimulator`.

Les setters clampent silencieusement les valeurs hors plage. Tous les champs sont `volatile`
pour la visibilité multi-thread.

Implémente `Serializable`.

---

### 3.6 Grid

Grille 2D de l'automate cellulaire. Gère la structure de données, l'initialisation aléatoire
et la sauvegarde/restauration de l'état initial.

**Convention d'indexation :** stockage interne `cells[y][x]`, mais toutes les méthodes publiques
acceptent les paramètres dans l'ordre `(x, y)`.

**Méthodes principales :**

| Méthode | Description |
|---------|-------------|
| `getCell(x, y)` / `setCell(x, y, cell)` | Accès direct aux cellules |
| `getNeighbors(x, y)` | Voisins de Moore (8 directions), ignore les hors-limites |
| `initRandom(densities...)` | Peuple aléatoirement selon 5 densités (somme ≤ 1) |
| `setFire(x, y)` | Enflamme une cellule si inflammable |
| `saveInitialState()` | Mémorise l'état courant comme état initial |
| `reset()` | Restaure l'état mémorisé (copie profonde) |
| `copyCells()` / `copyInitialCells()` | Copies profondes pour snapshot |
| `restoreState(cells, initialCells)` | Restaure les deux tableaux depuis un snapshot |

---

### 3.7 FireSimulator

Moteur de simulation. Orchestre les ticks, la propagation du feu, l'historique et les snapshots.

#### Déroulement d'un tick

```
tick()
 ├── pushHistory()          ← sauvegarde l'état courant (max 100 frames)
 ├── takeSnapshot()         ← capture CellState[][] pour les décisions
 ├── collectIgnitions()     ← calcule stochastiquement les nouveaux feux
 ├── apply ignitions        ← enflamme atomiquement toutes les cellules cibles
 ├── applyExtinctions()     ← décrémente les compteurs, éteint les épuisées
 └── notifyListeners()      ← notifie tous les SimulationListener
```

#### Formule de propagation

```
windFactor     = 1.0 + (windStrength / MAX_WIND_FACTOR) × cos(θ)
humidityFactor = 1.0 − humidity / 100.0
P              = clamp(base × windFactor × humidityFactor, 0.0, 1.0)
```

où θ est l'angle entre le vecteur vent et le vecteur source→cible.
Le double-buffering (`takeSnapshot`) garantit que toutes les décisions sont basées
sur l'état *avant* le tick.

#### Historique (step-back)

`pushHistory()` stocke jusqu'à **100** états en pile (`Deque<Cell[][]>`). `stepBack()` dépile
le dernier état et le réapplique à la grille.

#### Snapshot (import/export)

- `createSnapshot()` — emballe grille + environnement + tick dans un `SimulationSnapshot`
- `applySnapshot(snapshot)` — restaure l'état, vide l'historique, notifie les listeners

---

### 3.8 SimulationListener

Interface observateur notifiée par `FireSimulator` :

```java
void onTick(int tick, Grid grid);
void onSimulationEnd();
```

Implémentée par `GridView`, `StatisticsPanel`, `ChartView`.

---

### 3.9 SimulationState

Énumération des états du contrôleur :

```
IDLE ──start()──► RUNNING ──pause()──► PAUSED
 ▲                    │                    │
 └─────stop()─────────┴────────────────────┘
```

---

### 3.10 SimulationSnapshot

Capture **immuable** et **sérialisable** de l'état complet d'une simulation :
- `Cell[][]` cells et initialCells (copies profondes)
- `WindDirection`, `windStrength`, `humidity`
- `tick` courant
- dimensions `width` × `height`

Utilisé par `SimulationIO` pour la persistance binaire.

---

### 3.11 SimulationIO

Utilitaire statique pour la persistance binaire (sérialisation Java) :

| Méthode | Description |
|---------|-------------|
| `save(File, SimulationSnapshot)` | Écrit le snapshot en binaire (`.simcel`) |
| `load(File)` | Lit et désérialise un fichier `.simcel` |

---

## 4. Contrôleur (`controller/`)

### 4.1 SimulationController

Orchestre le cycle de vie de la simulation. Toutes les méthodes publiques sont `synchronized`.

| Méthode | Description |
|---------|-------------|
| `start()` | Lance la boucle dans un `ScheduledExecutorService` |
| `pause()` | Suspend la boucle sans réinitialiser |
| `stop()` | Arrête et remet le compteur à 0 |
| `reset()` | `stop()` + `grid.reset()` + `clearHistory()` |
| `step()` | Exécute un tick manuellement (hors `RUNNING`) |
| `stepBack()` | Revient un tick en arrière (hors `RUNNING`) |
| `setTickDelay(ms)` | Ajuste l'intervalle ; redémarre si `RUNNING` |
| `saveToFile(File)` | Crée un snapshot et le sérialise |
| `loadFromFile(File)` | Désérialise, applique le snapshot, passe en `PAUSED` |

---

## 5. Vue (`view/`)

### 5.1 GridView

Canvas JavaFX qui affiche la grille. Implémente `SimulationListener`.

- Les couleurs sont pré-calculées en tableaux (`STATE_COLORS[]`, `TYPE_SAIN_COLORS[]`) pour éviter
  les allocations à chaque tick.
- Le rendu est threadé : capture d'un snapshot de couleurs sur le thread de simulation,
  puis `Platform.runLater()` pour dessiner.
- `getCellAt(pixelX, pixelY)` convertit les coordonnées écran en coordonnées grille.
- `refresh(Grid)` force un redessin immédiat (utilisé après reset ou chargement).

---

### 5.2 StatisticsPanel

Barre horizontale (`HBox`) affichant en temps réel :
- Numéro du tick
- Nombre de cellules saines, en feu, brûlées
- Taux de destruction (% de cellules brûlées sur le total)

Comptages sur le thread de simulation, mise à jour labels sur le thread JavaFX.

---

### 5.3 ChartView

Canvas personnalisé (sans `javafx.charts`) traçant trois courbes en temps réel :

| Courbe | Couleur | Données |
|--------|---------|---------|
| Sains  | vert    | Nombre de cellules `SAIN` |
| En feu | orange  | Nombre de cellules `EN_FEU` |
| Brûlés | gris    | Nombre de cellules `BRULE` |

- Fenêtre glissante de **300 points** (`MAX_POINTS`).
- `observedMax` adapte automatiquement l'échelle de l'axe Y.
- `removeLastPoint()` synchronisé avec `stepBack()` pour rester cohérent.
- `BG_COLOR` et `MIN_OBSERVED_MAX` extraits en constantes nommées.

---

### 5.4 LegendPanel

Barre horizontale figée (haut de fenêtre) affichant :
- Les 5 types de terrain avec leur couleur `SAIN`
- Les états spéciaux : VIDE, EN FEU, BRÛLÉ, EAU, ROCHER

---

### 5.5 WindIndicator

Composant `VBox` affichant :
- Une **boussole** canvas (96×96 px) avec flèche de vent directionnelle
  (longueur ∝ intensité, orange si vent actif, gris si nul)
- Les labels de direction (N/NE/E…), le label actif en orange
- Cinq indicateurs **●/○** pour la force du vent
- Le taux d'humidité

`refresh(Environment)` met à jour la boussole et les labels dynamiquement.

Constantes nommées : `SIZE`, `R`, `MAX_WIND_STRENGTH`, `ARROWHEAD_ANGLE`,
`ARROWHEAD_LENGTH`, `LABEL_RADIUS_EXTRA`, et les couleurs `COLOR_*`.

---

### 5.6 ConsoleView

Vue texte ANSI pour le mode headless :

| État | Rendu |
|------|-------|
| VIDE   | `░░` gris |
| SAIN   | `██` vert |
| EN_FEU | `▓▓` rouge |
| BRULE  | `  ` fond noir |
| EAU    | `~~` bleu |
| ROCHER | `▒▒` blanc |

- `render(tick, grid)` efface le terminal et redessine en un seul `print`.
- `startCommandListener(env, ctrl)` lance un thread daemon lisant stdin.
- Commandes disponibles : `wind <dir> <force>`, `humidity <val>`, `speed <ms>`, `help`.
- Les bornes de validation (`0–5`, `0–100`) sont lues depuis `Environment.MAX_WIND`
  et `Environment.MAX_HUMIDITY`.

---

### 5.7 MainWindow

Fenêtre principale JavaFX (`BorderPane`) :

```
┌─────────────────────────────────────────────────┐
│  LegendPanel (Top)                              │
├────────────────────────────┬────────────────────┤
│                            │ Contrôles          │
│    GridView (Center)       │ Paramètres météo   │
│    zoom + pan + édition    │ Edition du terrain │
│                            │                    │
├────────────────────────────┤                    │
│ ChartView │ WindIndicator  │                    │
├───────────────────────────────────────────────-─┤
│  StatisticsPanel (Bottom)                       │
└─────────────────────────────────────────────────┘
```

**Fonctionnalités :**

| Fonctionnalité | Détail |
|----------------|--------|
| Zoom | Molette souris, centré sur le curseur, plage `[0.2×, 8×]` |
| Déplacement | Clic-glisser ; double-clic reset la vue |
| Mode édition | 9 outils : 5 terrains + Vide, Eau, Rocher, Foyer |
| Pinceau | Clic ou glisser sur la grille |
| Paramètres météo | ComboBox direction + sliders force/humidité avec `windIndicator.refresh()` |
| Sauvegarde | Bouton 💾, FileChooser `.simcel` |
| Chargement | Bouton 📂, FileChooser `.simcel`, rafraîchit toute la vue |
| Auto-stop | S'arrête automatiquement quand plus aucune cellule `EN_FEU` |

**Méthodes de construction :** `buildGridPane()`, `buildRightPanel()`,
`buildEnvSection()`, `buildEditSection()`.

---

## 6. Point d'entrée (`Main`)

`Main extends Application` — routage GUI/CLI :

```
main(args)
 ├── "--headless" présent → runCLI(args)
 └── sinon               → launch() → start(Stage)
```

**Mode graphique :** grille `50×30`, densités 60/20/10/5/5 %, foyer central, délai 200 ms.

**Mode console — arguments :**

| Argument | Défaut | Description |
|----------|--------|-------------|
| `--width <n>`            | 40  | Largeur de la grille |
| `--height <n>`           | 20  | Hauteur de la grille |
| `--ticks <n>`            | 100 | Ticks maximum |
| `--delay <ms>`           | 200 | Délai entre ticks |
| `--wind-direction <dir>` | N   | Direction initiale du vent |
| `--wind-strength <0-5>`  | 0   | Intensité initiale du vent |
| `--humidity <0-100>`     | 0   | Humidité initiale |

Toutes les densités et dimensions par défaut sont extraites en constantes nommées
(`GUI_WIDTH`, `DENSITY_FORET`, etc.).

---

## 7. Algorithme de simulation

### Propagation stochastique

Pour chaque cellule `EN_FEU` (basé sur le snapshot *avant* le tick) :
1. Pour chaque voisin `SAIN` dans le voisinage de Moore :
2. Calculer `P = base × windFactor × humidityFactor`
3. Tirer `random.nextDouble()` — si `< P`, ajouter le voisin à `toIgnite`
4. Appliquer toutes les ignitions atomiquement (pas de double-comptage grâce au `LinkedHashSet`)

### Extinction

Pour chaque cellule `EN_FEU` dans le snapshot :
- Décrémenter `remainingBurnTime`
- Si `== 0` → `burnOut()` → passe à `BRULE`

### Double-buffering

`takeSnapshot()` capture un tableau `CellState[width][height]` en début de tick.
Toutes les décisions (ignition, extinction) se basent sur ce snapshot immuable,
garantissant que l'ordre de parcours des cellules n'influence pas le résultat.

---

## 8. Import / Export

La persistance utilise la **sérialisation binaire Java** (format `.simcel`).

### Ce qui est sauvegardé

- État complet de toutes les cellules (type + état + temps restant)
- État initial (utilisé par le bouton Réinitialiser)
- Paramètres météo (direction, force du vent, humidité)
- Numéro du tick courant

### Flux de sauvegarde

```
btnSave → SimulationController.saveToFile(file)
            └── FireSimulator.createSnapshot()
                    ├── Grid.copyCells()
                    ├── Grid.copyInitialCells()
                    └── SimulationIO.save(file, snapshot)
```

### Flux de chargement

```
btnLoad → SimulationController.loadFromFile(file)
            ├── SimulationIO.load(file)          → SimulationSnapshot
            ├── FireSimulator.applySnapshot()
            │       ├── Grid.restoreState()
            │       ├── Environment setters
            │       └── notifyListeners()
            └── Vue : gridView.refresh(), chartView.clear(),
                       statisticsPanel.reset(), windIndicator.refresh()
```

Les dimensions de la grille du snapshot doivent correspondre à la grille courante
(sinon `IllegalArgumentException`).

---

## 9. Diagramme des dépendances

```
Main
 ├── Grid ──────────────────── Cell ── CellType / CellState
 │    └── (copyCells, restore)
 ├── Environment ──────────── WindDirection
 ├── FireSimulator
 │    ├── uses Grid, Environment
 │    ├── notifies SimulationListener
 │    ├── createSnapshot() ──► SimulationSnapshot
 │    └── applySnapshot()  ◄── SimulationSnapshot
 ├── SimulationController
 │    ├── drives FireSimulator
 │    ├── saveToFile() ──► SimulationIO
 │    └── loadFromFile() ◄── SimulationIO
 └── MainWindow
      ├── GridView (SimulationListener)
      ├── StatisticsPanel (SimulationListener)
      ├── ChartView (SimulationListener)
      ├── LegendPanel
      └── WindIndicator
```

---

## 10. Scripts de lancement

| Script | Description |
|--------|-------------|
| `run-gui.sh`      | Lance le mode graphique JavaFX |
| `run-headless.sh` | Lance le mode console (passer `--headless` + options) |

**Compilation en ligne de commande :**
```bash
mvn compile
mvn package       # génère un JAR exécutable
```

**Génération de la JavaDoc :**
```bash
mvn javadoc:javadoc
# Résultat dans target/site/apidocs/
```
