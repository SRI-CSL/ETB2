default:
	javac -d . etbDL/engine/* etbDL/utils/* etbDL/etbDatalog.java etbDL/etbDatalogEngine.java etbDL/statement/* etbDL/output/* etbDL/services/* etbCS/utils/* etbCS/etcServer.java etbCS/clientMode.java

all:
	javac -d . wrappers/* etbDL/engine/* etbDL/utils/* etbDL/etbDatalog.java etbDL/etbDatalogEngine.java etbDL/statement/* etbDL/output/* etbDL/services/* etbCS/utils/* etbCS/etcServer.java etbCS/clientMode.java

clean:
	rm -rf etb

