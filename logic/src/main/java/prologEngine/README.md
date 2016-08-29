http://www.cse.unt.edu/~tarau/research/2016/prologEngine.zip
License: Apache 2.0

usage:

go.sh perms

go.sh queens

or

go.sh <any pure Prolog program in directory progs>

a swi prolog script first compiles the code
then dload in Engine loads it in memory from
where run in Engine starts executing it

todo:

- design compiler
- faster runtime:
    memory efficiency - eg recursive loop, LCO
    code to be mem copied with ptrs to var/ref cells to be relocated?

more thoughts

no symbol tables:

a symbol is just a ground array of ints

instead of a symbol table we would have a "ground cache"

when a non-ground compound tries to unify with a ground, the ground is
expanded to the heap

when a ground unifies with a ground - it's just handle equality
when a var unifies with a ground it just points to it - as if it were a constant

primitive types:

int
ground
var (U+V)
ref
array

see main code (~1000lines) in Engine.java

pl2nl.pl compiles a .pl file to its .nl equivalent, ready to run by
the java based runtime system

natint.pl emulates its work by compileing .nl files to Prolog clauses


Enjoy,

Paul Tarau

May 2016
