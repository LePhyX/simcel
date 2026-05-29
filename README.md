# Simulation de Propagation de Feu

Automate cellulaire 2D simulant la propagation d'un incendie dans un environnement naturel.

Projet réalisé dans le cadre du cours de Génie Logiciel — cursus ingénieur informatique (CY Tech).

---

## Fonctionnalités principales

- Grille 2D paramétrable avec 6 états de cellule (VIDE, SAIN, EN_FEU, BRULÉ, EAU, ROCHER)
- 5 types de terrain avec inflammabilité et durée de combustion propres
- Paramètres environnementaux : vent (8 directions, intensité 0–5) et humidité (0–100 %)
- Contrôle de la simulation : démarrer, pause, step, stop, reset
- Interface graphique (JavaFX) et mode console headless (CLI)
- Statistiques en temps réel et courbe d'évolution du feu

---

## Prérequis

- Java 17+
- Maven 3.8+ ou Gradle 8+
- JavaFX 17+ (géré via Maven/Gradle)

---

## Installation

```bash
git clone https://github.com/LePhyX/simcel.git
cd simcel
```

---

## Lancement

### Mode GUI

```bash
mvn javafx:run
```

### Mode console (headless)

```bash
bash launchers/run-cli.sh
```

Arguments disponibles :

| Argument            | Description                                            | Défaut |
| ------------------- | ------------------------------------------------------ | ------ |
| `--width`           | Largeur de la grille                                   | 40     |
| `--height`          | Hauteur de la grille                                   | 20     |
| `--ticks`           | Nombre maximum de ticks                                | 100    |
| `--delay`           | Délai entre chaque tick (ms)                           | 200    |
| `--wind-direction`  | Direction du vent : `N` `NE` `E` `SE` `S` `SO` `O` `NO` | `N`  |
| `--wind-strength`   | Force du vent, entier dans `[0, 5]` (0 = calme, 5 = tempête) | `0` |

Exemples :

```bash
# Vent fort vers le Nord — le feu remonte la grille
bash launchers/run-cli.sh --wind-direction N --wind-strength 5

# Vent diagonal NE modéré
bash launchers/run-cli.sh --wind-direction NE --wind-strength 3

# Sans vent — propagation isotrope (comportement par défaut)
bash launchers/run-cli.sh --wind-strength 0
```

> **Effet du vent :** la probabilité d'inflammation d'une cellule voisine est multipliée par un facteur `1 + (force/5) × cos θ`, où θ est l'angle entre la direction du vent et le vecteur source→cible. Avec une force de 5, la probabilité est doublée dans le sens du vent et nulle en sens opposé.

---

## Architecture

L'application suit le patron **MVC** :

| Couche     | Package      | Rôle                                                                  |
| ---------- | ------------ | --------------------------------------------------------------------- |
| Modèle     | `model`      | Logique métier : `Cell`, `Grid`, `Environment`, `FireSimulator`       |
| Vue        | `view`       | Affichage : `GridView`, `ConsoleView`, `StatisticsPanel`, `ChartView` |
| Contrôleur | `controller` | Orchestration : `SimulationController`, `ConfigController`            |

---

## Diagrammes UML

Disponibles dans [`docs/uml/`](docs/uml/).

---

## Convention de branches

| Préfixe            | Usage                |
| ------------------ | -------------------- |
| `main`             | Code stable livrable |
| `dev` ou `develop` | Intégration continue |

---

## Équipe

- [Rayân MOUAHID](https://github.com/LePhyX)
- [Sofiane HOUMMASS](https://github.com/sofianehms)
