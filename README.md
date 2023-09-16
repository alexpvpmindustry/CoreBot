this is forked from anuke's corebot

warning to future alex/developers:
do not pull the parent repo into this master whenever there is an update.
this is because the hard-coded constants are different.

pull into a fork, and then PR to take/update the necessary changes.

to run this:

clone this repo *and* anuke/Mindustry
navigate to anuke/Mindustry and run:
./gradlew build
./gradlew core:build
./gradlew core:dist
./gradlew desktop:build
./gradlew desktop:dist
navigate to this repo's directory
change the bot channel info and the discord bot invite
run:
./gradlew dist and/or ./gradlew jar
copy the build/libs/CoreBot.jar file into directory of Mindustry
java -jar CoreBot.jar