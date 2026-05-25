#!/bin/bash

# Simulation de Propagation de Feu — lancement en mode CLI
# Usage : ./launchers/run-cli.sh [--width W] [--height H] [--ticks T] [--delay D]
#
# Options :
#   --width   W  Largeur de la grille       (défaut : 40)
#   --height  H  Hauteur de la grille       (défaut : 20)
#   --ticks   T  Nombre de ticks maximum    (défaut : 100)
#   --delay   D  Délai entre chaque tick ms (défaut : 200)

set -e

cd "$(dirname "$0")/.."

mvn compile -q
mvn exec:java -q -Dexec.args="--headless $*"
