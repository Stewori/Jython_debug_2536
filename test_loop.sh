#!/bin/bash
x=1
#while java -cp jython.jar:build/JyNI.jar org.python.util.jython JyNI-Demo/src/JyNINumPyTest.py
while ant regrtest
do
  echo "tested $x times"
  x=$(( $x + 1 ))
  echo "======================================="
  echo ""
done

