#!/usr/bin/env sh

##############################################################################
##
##  Copying files for uncompiled execution
##
##############################################################################

echo "Copying files for uncompiled execution"

#rm assets/block_colors.png
#rm assets/sprites.aatls
rm CoreBot.jar

#cp -f ../Mindustry/core/assets/sprites/block_colors.png assets/block_colors.png
#cp -f ../Mindustry/core/assets/sprites/sprites.aatls assets/sprites.aatls
#cp -f ../Mindustry/core/assets-raw/sprites-out/* assets/sprites-out/
cp -f build/libs/CoreBot.jar CoreBot.jar

echo "done Copying files for uncompiled execution"

sleep 1