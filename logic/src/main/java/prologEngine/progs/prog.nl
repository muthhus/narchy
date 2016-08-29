mylist X if X lists 1 2 3. 

mynum X if X holds s Y and Y holds s 0.
  
sum 0 X X.
sum SX Y SZ if 
  SX holds s X and 
  SZ holds s Z and
  sum X Y Z.
  
app nil Xs Xs.
app XXs Ys XZs if
  XXs holds list X Xs and
  XZs holds list X Zs and
  app Xs Ys Zs.

call F X if F X.
 
test R if mynum X and sum X X R.
test Rs if mylist Xs and app Xs Xs Rs.

goal X if call test X.
