#
# Makefile for CA on X
#
LIBPATH = -L/usr/X11R6/lib
#INCPATH = -I/usr/X11R6/include
#INCPATH = -I/usr/openwin/include -I/usr/include 
#LIBPATH = -L/usr/openwin/lib -L/usr/lib
EXEC    = ca
LIBS    = -lX11 -lm
OBJECTS = ca.o SimWindow.o codi.o GA.o app.o
SOURCES = ca.c SimWindow.c codi.c GA.c app.c
OPTIONS = -O0

#  OPTIONS = -g -Wall
#
# Rule for gernerating c objct code
# this detects a change in the object files 
# but not thier header files.
#
.c.o:   
	gcc -c $(INCPATH) $(LIBPATH) $(OPTIONS) $<

#
# Compiler Call
#

# # The old version:

all:     $(EXEC)

$(EXEC): $(OBJECTS)
	gcc -o $(EXEC) $(OBJECTS) $(LIBPATH) $(LIBS) $(OPTIONS)

ca.o    : ca.c ca.h CA-Def-Type.c 
	gcc -c ca.c $(INCPATH) $(OPTIONS)

SimWindow.o : SimWindow.c SimWindow.h
	gcc -c SimWindow.c $(INCPATH) $(OPTIONS)

codi.o  : codi.c codi.h CA-Def-Type.c
	gcc -c codi.c $(INCPATH) $(OPTIONS)

GA.o    : GA.c GA.h CA-Def-Type.c
	gcc -c GA.c $(INCPATH) $(OPTIONS)

app.o   : app.c app.h CA-Def-Type.c
	gcc -c app.c $(INCPATH) $(OPTIONS)


# The make calls makedepend and then itself again version:

# all:
#       @makedepend $(INCPATH) $(SOURCES) # @ for silent
#       make $(EXEC)
# 
# depend:
#       mand of Makefile
#
# DO NOT DELETE THIS LINE -- make depend depends on it.

