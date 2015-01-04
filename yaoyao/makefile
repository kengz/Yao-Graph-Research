JFLAGS = -g
JC = javac
JVM= java 
FILE=
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
	Yaoyao.java
	
	
	

MAIN = Yaoyao

default: classes run clean

classes: $(CLASSES:.java=.class)
 

run: $(MAIN).class
	$(JVM) $(MAIN)

clean:
	$(RM) *.class *.java~

