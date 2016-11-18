# Pacman
Author: Henry Friedlander

Single player Pacman game with GUI. Java program using Swing.

Used Object Oriented Programming to create a Pacman game, where the goal is to collect all of the pink and red balls. Pacman is controlled by the arrow keys. There are two enemies each of whom use an AI that generated with manhattan walks. There is a level editor so that user can create levels via a text file.

Level 1

![alt text](https://github.com/henryfriedlander/Pacman/blob/master/docs/pics/pacman.JPG)

Level 2 (thanks to #peteflorence)

![alt text](https://raw.githubusercontent.com/peteflorence/Pacman/master/Pacman/MITpacman.png "MIT level :)")

### Quickstart with Eclipse

Run with Eclipse project

### Quickstart without Eclipse

```
cd Pacman/src
for i in *.java; do javac *.java; done
java PacmanGame

```

To run a different level, for example:


```
java PacmanGame level2.txt
```


