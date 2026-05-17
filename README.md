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

> Instructions à compléter au fil du projet.

### Mode GUI

```bash
mvn javafx:run
```

### Mode console (headless)

```bash
mvn package
java -jar target/simcel.jar --width 50 --height 50 --ticks 200 --delay 100
```

Arguments disponibles :

| Argument | Description | Défaut |
|----------|-------------|--------|
| `--width` | Largeur de la grille | 50 |
| `--height` | Hauteur de la grille | 50 |
| `--ticks` | Nombre maximum de ticks | 100 |
| `--delay` | Délai entre chaque tick (ms) | 200 |

---

## Tests

```bash
mvn test
```

---

## Architecture

L'application suit le patron **MVC** :

| Couche | Package | Rôle |
|--------|---------|------|
| Modèle | `model` | Logique métier : `Cell`, `Grid`, `Environment`, `FireSimulator` |
| Vue | `view` | Affichage : `GridView`, `ConsoleView`, `StatisticsPanel`, `ChartView` |
| Contrôleur | `controller` | Orchestration : `SimulationController`, `ConfigController` |

---

## Diagrammes UML

Disponibles dans [`docs/uml/`](docs/uml/).

---

## Convention de branches

| Préfixe | Usage |
|---------|-------|
| `main` | Code stable livrable |
| `develop` | Intégration continue |
| `feature/<nom>` | Nouvelle fonctionnalité |
| `fix/<nom>` | Correction de bug |

---

## Équipe

- [Rayân MOUAHID](https://github.com/LePhyX)
- [Sofiane HOUMMASS](https://github.com/sofianehms)
