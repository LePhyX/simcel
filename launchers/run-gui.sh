#!/bin/bash

# Simulation de Propagation de Feu — lancement en mode interface graphique
# Usage : ./launchers/run-gui.sh

set -e # Exit on error

cd "$(dirname "$0")/.." # Se placer à la racine du projet pour que Maven puisse trouver le pom.xml

mvn compile -q # Compiler le projet sans afficher les logs détaillés
mvn exec:java -q  # Lancer l'application en mode graphique (pas de --headless)
