#!/bin/bash

# Simulation de Propagation de Feu — lancement en mode CLI
# Usage : ./launchers/run-cli.sh [--width W] [--height H] [--ticks T] [--delay D] [--wind-direction DIR] [--wind-strength S] [--humidity H]
#
# Options :
#   --width          W    Largeur de la grille                  (défaut : 40)
#   --height         H    Hauteur de la grille                  (défaut : 20)
#   --ticks          T    Nombre de ticks maximum               (défaut : 100)
#   --delay          D    Délai entre chaque tick ms            (défaut : 200)
#   --wind-direction DIR  Direction du vent : N NE E SE S SO O NO (défaut : N)
#   --wind-strength  S    Force du vent, entier dans [0,5]      (défaut : 0)
#   --humidity       H    Taux d'humidité, entier dans [0,100]  (défaut : 0)

set -e

cd "$(dirname "$0")/.."

mvn compile -q
mvn exec:java -q -Dexec.args="--headless $*"
