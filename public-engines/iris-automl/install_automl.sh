#!/bin/sh
###############################################
# Marvin AutoML installation Script           #
###############################################
cmd=(dialog --title "Marvin AutoML" --separate-output --checklist "Select tools:" 22 76 16)
options=(1 "auto-sklearn" off    # any option can be set to default to "on"
         2 "h2o AutoML" off
         3 "TPOT" off)
choices=$("${cmd[@]}" "${options[@]}" 2>&1 >/dev/tty)
clear
for choice in $choices
do
    case $choice in
        1)
            echo "Instaling auto-sklearn..."
            wget https://raw.githubusercontent.com/automl/auto-sklearn/master/requirements.txt \
                 | xargs -n 1 -L 1 pip install
            pip install auto-sklearn
            ;;
        2)
            echo "Installing h2o..."
            pip install requests
            pip install tabulate
            pip install scikit-learn
            pip install http://h2o-release.s3.amazonaws.com/h2o/rel-yau/3/Python/h2o-3.26.0.3-py2.py3-none-any.whl
            wget http://h2o-release.s3.amazonaws.com/h2o/rel-yau/3/h2o-3.26.0.3.zip
            unzip h2o-3.26.0.3.zip
            rm h2o-3.26.0.3.zip
            ;;
        3)
            echo "Installing TPOT..."
            pip install tpot
            ;;
    esac
done