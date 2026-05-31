# SimCel — Explication complète du code

## Table des matières

1. [Vue d'ensemble du projet](#1-vue-densemble-du-projet)
2. [Architecture générale](#2-architecture-générale)
3. [Le modèle (`model/`)](#3-le-modèle-model)
   - [CellState](#31-cellstate)
   - [CellType](#32-celltype)
   - [Cell](#33-cell)
   - [WindDirection](#34-winddirection)
   - [Environment](#35-environment)
   - [Grid](#36-grid)
   - [FireSimulator](#37-firesimulator)
   - [SimulationListener](#38-simulationlistener)
   - [SimulationState](#39-simulationstate)
4. [Le contrôleur (`controller/`)](#4-le-contrôleur-controller)
   - [SimulationController](#41-simulationcontroller)
5. [La vue (`view/`)](#5-la-vue-view)
   - [ConsoleView](#51-consoleview)
6. [Point d'entrée (`Main`)](#6-point-dentrée-main)
7. [Configuration du module (`module-info.java`)](#7-configuration-du-module-module-infojava)
8. [Algorithme de simulation pas à pas](#8-algorithme-de-simulation-pas-à-pas)
9. [Diagramme des dépendances entre classes](#9-diagramme-des-dépendances-entre-classes)

---

## 1. Vue d'ensemble du projet

SimCel est une **simulation de propagation d'incendie** fondée sur le modèle des **automates cellulaires**. Le terrain est découpé en une grille 2D de cellules. À chaque pas de temps (tick), chaque cellule en feu tente de propager l'incendie à ses voisines selon une probabilité dépendant du type de terrain.

Le projet propose deux modes de lancement :

- **Mode console** (`--headless`) : animation textuelle ANSI dans le terminal.
- **Mode graphique** (défaut) : squelette JavaFX prêt à être enrichi d'une interface.

---

## 2. Architecture générale

Le projet suit le patron **MVC (Modèle - Vue - Contrôleur)** :

```
com.simcel
│
├── model/          ← Données et logique métier (automate cellulaire)
│   ├── CellState        État d'une cellule (SAIN, EN_FEU, BRULE…)
│   ├── CellType         Type de terrain (FORET, PRAIRIE…)
│   ├── Cell             Unité élémentaire de la grille
│   ├── WindDirection    Direction du vent (N, NE, E…)
│   ├── Environment      Conditions météo (vent, humidité)
│   ├── Grid             Grille 2D de cellules
│   ├── FireSimulator    Moteur de simulation (algorithme tick)
│   ├── SimulationListener  Interface Observer
│   └── SimulationState  État du cycle de vie (IDLE, RUNNING, PAUSED)
│
├── controller/     ← Pilotage de la boucle de simulation
│   └── SimulationController   Démarre, pause, arrête la simulation
│
├── view/           ← Affichage
│   └── ConsoleView    Vue console ANSI
│
└── Main            ← Point d'entrée (JavaFX ou CLI)
```

Le **modèle** ne connaît ni la vue ni le contrôleur.  
La **vue** ne connaît que le modèle (lecture seule).  
Le **contrôleur** orchestre le modèle et notifie la vue via le patron **Observer**.

---

## 3. Le modèle (`model/`)

### 3.1 `CellState`

```java
public enum CellState {
    VIDE, SAIN, EN_FEU, BRULE, EAU, ROCHER
}
```

**Rôle :** représente l'état courant d'une cellule.

| Constante | Signification                 | Inflammable ? | Propage le feu ? |
| --------- | ----------------------------- | :-----------: | :--------------: |
| `VIDE`    | Terrain vide, sans végétation |      Non      |       Non        |
| `SAIN`    | Végétation saine              |    **Oui**    |       Non        |
| `EN_FEU`  | En combustion                 |      Non      |     **Oui**      |
| `BRULE`   | Entièrement consumé           |      Non      |       Non        |
| `EAU`     | Plan d'eau (ignifuge)         |      Non      |       Non        |
| `ROCHER`  | Roche (ignifuge)              |      Non      |       Non        |

Chaque constante porte :

- `color` : couleur hexadécimale pour l'interface graphique.
- `isFlammable()` : retourne `true` uniquement pour `SAIN`.
- `isSpreading()` : retourne `true` uniquement pour `EN_FEU`.

---

### 3.2 `CellType`

```java
public enum CellType {
    FORET(0.7, 8), PRAIRIE(0.9, 3), BROUSSAILLES(0.5, 5),
    ZONE_HUMIDE(0.2, 12), ZONE_URBAINE(0.3, 6)
}
```

**Rôle :** représente le **type de terrain** d'une cellule. Contrairement à `CellState`, le type ne change jamais au cours de la simulation.

Chaque type porte deux paramètres physiques :

| Type           | Inflammabilité | Durée de combustion |
| -------------- | :------------: | :-----------------: |
| `FORET`        |      70 %      |       8 ticks       |
| `PRAIRIE`      |      90 %      |       3 ticks       |
| `BROUSSAILLES` |      50 %      |       5 ticks       |
| `ZONE_HUMIDE`  |      20 %      |      12 ticks       |
| `ZONE_URBAINE` |      30 %      |       6 ticks       |

- **`inflammability`** : probabilité de base que cette cellule s'enflamme quand un voisin brûle.
- **`burnDuration`** : nombre de ticks avant extinction complète une fois en feu.

---

### 3.3 `Cell`

```java
public class Cell {
    private CellState state;          // état courant (mutable)
    private final CellType type;      // type de terrain (immuable)
    private int remainingBurnTime;    // ticks de combustion restants
}
```

**Rôle :** unité élémentaire de la grille. C'est l'objet central de la simulation.

**Méthodes importantes :**

- **`ignite()`** : enflamme la cellule si elle est `SAIN`. Réinitialise `remainingBurnTime` à la valeur par défaut du type.
- **`decrementBurnTime()`** : décrémente le compteur à chaque tick (jamais en dessous de 0).
- **`burnOut()`** : marque la cellule comme `BRULE` et remet le compteur à 0.
- **`copy()`** : crée une copie profonde utilisée pour la sauvegarde de l'état initial et le double buffering.

**Cycle de vie d'une cellule :**

```
SAIN ──ignite()──► EN_FEU ──burnOut()──► BRULE
```

---

### 3.4 `WindDirection`

```java
public enum WindDirection {
    N(0,-1), NE(1,-1), E(1,0), SE(1,1),
    S(0,1), SO(-1,1), O(-1,0), NO(-1,-1)
}
```

**Rôle :** encode les 8 directions cardinales et inter-cardinales du vent sous forme de vecteurs unitaires `(dx, dy)` dans le repère écran (axe Y vers le bas).

Ce vecteur est prévu pour être utilisé dans `FireSimulator.computeInflammationProbability()` afin de favoriser la propagation dans le sens du vent.

---

### 3.5 `Environment`

```java
public class Environment {
    private WindDirection direction;  // direction du vent
    private int windStrength;         // intensité [0..5]
    private int humidity;             // humidité [0..100]
}
```

**Rôle :** regroupe les **conditions météorologiques globales** de la simulation. Toutes les cellules partagent le même environnement.

- **`windStrength`** : clampé dans `[0, 5]` par le setter (0 = calme, 5 = tempête).
- **`humidity`** : clampé dans `[0, 100]` par le setter.

Ces données sont disponibles dans `FireSimulator` via le champ `environment`, mais leur intégration dans le calcul de probabilité est un **développement futur prévu**.

---

### 3.6 `Grid`

```java
public class Grid {
    private final int width, height;
    private final Cell[][] cells;         // état courant
    private final Cell[][] initialCells;  // état sauvegardé pour reset
}
```

**Rôle :** structure de données 2D — c'est le **terrain de jeu** de la simulation. Elle ne contient aucune logique de simulation.

**Convention interne :** les tableaux sont indexés `[y][x]` (ligne, colonne), mais toutes les méthodes publiques respectent l'ordre `(x, y)` pour la lisibilité.

**Méthodes importantes :**

- **`getNeighbors(x, y)`** : retourne les 8 voisins de Moore d'une cellule (les positions hors limites sont ignorées). Les cellules de coin ont 3 voisins, les cellules de bord en ont 5.
- **`initRandom(...)`** : peuple la grille aléatoirement en répartissant les types de terrain selon des densités passées en paramètres (somme ≤ 1.0). Mémorise l'état résultant dans `initialCells`.
- **`reset()`** : restaure `cells` à partir de `initialCells` par copie profonde — aucune référence n'est partagée.
- **`setFire(x, y)`** : déclenche un foyer en appelant `Cell.ignite()` sur la cellule cible.

**Exemple d'initialisation :**

```java
grid.initRandom(0.6, 0.2, 0.1, 0.05, 0.05);
// 60% forêt, 20% prairie, 10% broussailles, 5% humide, 5% urbain
grid.setFire(width / 2, height / 2);  // foyer au centre
```

---

### 3.7 `FireSimulator`

**Rôle :** c'est le **cœur algorithmique** de la simulation. Il exécute chaque tick et notifie les observateurs.

#### Algorithme du tick (double buffering)

Le double buffering garantit que toutes les décisions de propagation sont basées sur l'état **avant** le tick, et non sur un état partiellement mis à jour (ce qui biaiserait la simulation selon l'ordre de parcours).

```
tick() {
  1. takeSnapshot()        → copie les états dans un tableau indépendant
  2. collectIgnitions()    → détermine quelles cellules s'enflamment
  3. appliquer ignitions   → appelle cell.ignite() sur les candidates
  4. applyExtinctions()    → décrémente les compteurs, éteint si nécessaire
  5. notifyListeners()     → informe les observateurs (vue, contrôleur…)
}
```

#### `collectIgnitions(snapshot)`

```java
// Pour chaque cellule EN_FEU dans le snapshot...
for chaque cellule (x, y) EN_FEU dans snapshot :
    pour chaque voisin SAIN de (x, y) :
        p = computeInflammationProbability(src, voisin)
        si random.nextDouble() < p :
            toIgnite.add(voisin)  // LinkedHashSet → pas de doublons
```

Le `LinkedHashSet` est crucial : une cellule entourée de plusieurs foyers ne peut être ajoutée qu'une seule fois, quelle que soit combien de voisins tentent de l'enflammer.

#### `computeInflammationProbability(src, tgt)`

Actuellement, retourne simplement `tgt.getType().getInflammability()`. Le paramètre `src` et l'objet `environment` sont disponibles pour une future implémentation de l'influence du vent et de l'humidité.

#### Optimisation : `Random` dédié vs `Math.random()`

`Math.random()` utilise en interne un `Random` statique **synchronisé** — problématique en contexte multi-thread. Le simulateur possède sa propre instance `private final Random random` non partagée.

---

### 3.8 `SimulationListener`

```java
public interface SimulationListener {
    void onTick(int tick, Grid grid);
    void onSimulationEnd();
}
```

**Rôle :** interface du patron **Observer**. Toute classe souhaitant réagir aux ticks (vue, statistiques, condition d'arrêt…) doit implémenter cette interface et s'enregistrer via `FireSimulator.addListener()`.

`onTick` est appelé **après** chaque tick, une fois tous les changements appliqués.

---

### 3.9 `SimulationState`

```java
public enum SimulationState { IDLE, RUNNING, PAUSED }
```

**Rôle :** représente l'état du **contrôleur** (pas de la grille). Utilisé par `SimulationController` pour autoriser ou refuser les transitions.

```
IDLE ──start()──► RUNNING ──pause()──► PAUSED
 ▲                    │                   │
 └──────stop()────────┴───────stop()──────┘
```

---

## 4. Le contrôleur (`controller/`)

### 4.1 `SimulationController`

**Rôle :** orchestre la **boucle de simulation** dans un thread séparé, avec contrôle start/pause/stop/step.

```java
public class SimulationController {
    private final FireSimulator simulator;
    private volatile SimulationState state;
    private volatile int currentTick;
    private volatile int tickDelay;
    private ScheduledExecutorService executor;
}
```

#### Thread safety

Toutes les méthodes publiques sont `synchronized` pour éviter les races conditions (ex. appel simultané de `start()` et `stop()`). Les champs `state`, `currentTick` et `tickDelay` sont `volatile` pour la visibilité entre threads.

#### `ScheduledExecutorService`

Le thread de simulation est géré par un `ScheduledExecutorService` à un seul thread. Il appelle `doTick()` toutes les `tickDelay` millisecondes via `scheduleAtFixedRate`.

#### Méthodes clés

| Méthode            | Effet                                                            |
| ------------------ | ---------------------------------------------------------------- |
| `start()`          | Crée l'executor et lance la boucle. No-op si déjà `RUNNING`.     |
| `pause()`          | Arrête l'executor sans réinitialiser `currentTick`.              |
| `step()`           | Exécute un seul tick manuellement (uniquement si non `RUNNING`). |
| `stop()`           | Arrête l'executor et remet `currentTick` à 0.                    |
| `reset()`          | Appelle `stop()` puis `grid.reset()`.                            |
| `setTickDelay(ms)` | Change la vitesse et redémarre la boucle si elle tourne.         |

#### Pourquoi `currentTick` dans le contrôleur et dans le simulateur ?

- `FireSimulator.currentTick` : compteur global, jamais réinitialisé, transmis aux listeners.
- `SimulationController.currentTick` : compteur local remis à 0 à chaque `stop()`, utile pour l'affichage du nombre de ticks d'une session.

---

## 5. La vue (`view/`)

### 5.1 `ConsoleView`

**Rôle :** affichage de la grille dans le terminal via les **séquences d'échappement ANSI**.

#### Constantes ANSI

```java
private static final String CLEAR      = "\033[H\033[2J";  // efface l'écran
private static final String GREEN_BOLD = "\033[1;92m";     // vert vif gras
private static final String RED_BOLD   = "\033[1;91m";     // rouge vif gras
// ...
```

Ces séquences sont universellement supportées sur Linux, macOS et Windows Terminal.

#### `render(tick, grid)`

1. Efface le terminal (`CLEAR`).
2. Appelle `printLegend()`.
3. Appelle `printStats(tick, grid)`.
4. Construit la représentation de la grille dans un `StringBuilder` (évite les appels `System.out.print` répétés qui feraient scintiller l'affichage).
5. Écrit tout en une seule fois.

#### `printStats(tick, grid)`

Parcourt la grille, compte les cellules par état (`SAIN`, `EN_FEU`, `BRULE`) et affiche :

```
Tick   42 │ Sains:   312 │ En feu:    15 │ Brûlés:   73 │ Destruction:  18.3%
```

Le taux de destruction est calculé par rapport au **total** des cellules (`width × height`).

#### `toColorBlock(cell)`

Associe chaque `CellState` à une représentation visuelle de deux caractères :

| État     | Symbole | Couleur   |
| -------- | ------- | --------- |
| `VIDE`   | `░░`    | Gris      |
| `SAIN`   | `██`    | Vert vif  |
| `EN_FEU` | `▓▓`    | Rouge vif |
| `BRULE`  | `  `    | Fond noir |
| `EAU`    | `~~`    | Bleu vif  |
| `ROCHER` | `▒▒`    | Blanc vif |

---

## 6. Point d'entrée (`Main`)

```java
public class Main extends Application {
    public static void main(String[] args) throws InterruptedException {
        if (args.length > 0 && args[0].equals("--headless")) {
            runCLI(args);
        } else {
            launch(args);  // démarre JavaFX
        }
    }
}
```

### Mode console (`runCLI`)

1. **Parsing des arguments** : les flags `--width`, `--height`, `--ticks`, `--delay` sont lus avec un pattern `args[++i]` pour sauter proprement la valeur après chaque flag.
2. **Initialisation** : création de la grille, de l'environnement, du simulateur, du contrôleur et de la vue.
3. **Listener d'arrêt** : un `SimulationListener` anonyme décrémente un `CountDownLatch` quand le nombre maximum de ticks est atteint ou qu'il n'y a plus de feu actif.
4. **Synchronisation** : le thread principal appelle `done.await()` et bloque jusqu'à ce que le listener décrépite le latch.
5. **Bilan final** : affichage des statistiques après arrêt.

### `hasActiveFire(grid)`

Méthode utilitaire qui parcourt la grille et retourne `true` dès qu'une cellule `EN_FEU` est trouvée. Sert à stopper la simulation automatiquement quand le feu s'éteint.

---

## 7. Configuration du module (`module-info.java`)

```java
module com.simcel {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.simcel to javafx.fxml;

    exports com.simcel;
    exports com.simcel.model;
    exports com.simcel.controller;
    exports com.simcel.view;
}
```

Définit `com.simcel` comme un **module Java nommé** (JPMS, Java 9+).

| Directive                         | Rôle                                                           |
| --------------------------------- | -------------------------------------------------------------- |
| `requires javafx.controls`        | Accès aux composants UI JavaFX (Button, Label…)                |
| `requires javafx.fxml`            | Accès au chargeur de fichiers `.fxml`                          |
| `opens com.simcel to javafx.fxml` | Autorise JavaFX à instancier les contrôleurs par **réflexion** |
| `exports com.simcel`              | Rend le package accessible aux autres modules                  |
| `exports com.simcel.model`        | Idem pour `model`                                              |
| `exports com.simcel.controller`   | Idem pour `controller`                                         |
| `exports com.simcel.view`         | Idem pour `view`                                               |

Sans `opens`, JavaFX ne pourrait pas instancier `Main` (ni les futurs contrôleurs FXML) par réflexion au moment du chargement.

---

## 8. Algorithme de simulation pas à pas

Voici ce qui se passe concrètement lors d'un appel à `FireSimulator.tick()` :

```
État initial (tick N) :
  . . F . .        F = EN_FEU
  . S S S .        S = SAIN
  . S S S .        . = VIDE

Étape 1 — Snapshot :
  snapshot[x][y] = état courant de chaque cellule
  (copie indépendante, pas de référence partagée)

Étape 2 — Collecte des ignitions :
  La cellule F(2,0) est EN_FEU dans le snapshot.
  Ses voisins SAIN : S(1,1), S(2,1), S(3,1)
  Pour chaque voisin :
    p = inflammabilité du type de terrain
    si random() < p → ajouter au LinkedHashSet

Étape 3 — Application des ignitions :
  Toutes les cellules du LinkedHashSet passent à EN_FEU
  (atomique : personne ne voit un état intermédiaire)

Étape 4 — Extinction :
  F(2,0) était EN_FEU dans le snapshot → decrementBurnTime()
  si remainingBurnTime == 0 → burnOut() → BRULE

Étape 5 — Notification :
  onTick(N+1, grid) appelé sur tous les listeners
```

---

## 9. Diagramme des dépendances entre classes

```
                    ┌──────────────┐
                    │    Main      │
                    └──────┬───────┘
                           │ crée
          ┌────────────────┼────────────────┐
          ▼                ▼                ▼
  ┌──────────────┐  ┌────────────┐  ┌────────────┐
  │SimulationCtrl│  │FireSimulat.│  │ConsoleView │
  └──────┬───────┘  └─────┬──────┘  └─────┬──────┘
         │ pilote          │ lit/écrit      │ lit
         └────────────────►│               │
                    ┌──────▼──────┐        │
                    │    Grid     │◄───────┘
                    └──────┬──────┘
                           │ contient
                    ┌──────▼──────┐
                    │    Cell     │
                    └──────┬──────┘
                           │ a un
              ┌────────────┼────────────┐
              ▼                         ▼
        ┌─────────┐               ┌──────────┐
        │CellState│               │CellType  │
        └─────────┘               └──────────┘

  FireSimulator ──► Environment (lit les conditions météo)
  Environment   ──► WindDirection (direction du vent)
  SimulationCtrl ──► SimulationState (état du cycle de vie)
  FireSimulator ──► SimulationListener (notifie les observateurs)
```
