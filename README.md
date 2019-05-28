# ETB2

ETB2 is a complete reengineering of the Evidential Tool Bus (ETB) using Java. ETB2 is a platform for defining and executing distributed workflows that produce claims supported by evidence. ETB uses Datalog as the workflow scripting language.

## Getting Started

These instructions will get you an ETB node up and running on your local machine for testing purposes.

### Prerequisites

A JDK is required to compile and run ETB2.

### Installation

1. Clone the **ETB2** project  

	```console
	$ git clone https://github.com/SRI-CSL/ETB2.git
	```

2. Go to the cloned directory and build **ETB2**

	```console
	$ cd ETB2 && ./build.sh
	```

	The text **<span style="color:green">ETB2 built successfully</span>** at the end of the building process signals success.

	```console
	javac -cp .:dependencies/commons-exec-1.3.jar:dependencies/commons-io-2.6.jar:dependencies/json-simple-2.1.2.jar:dependencies/org.json.jar -d . etbDL/engine/* etbDL/utils/* etbDL/etbDatalog.java etbDL/etbDatalogEngine.java etbDL/statement/* etbDL/output/* etbDL/services/* etbCS/utils/* etbCS/etbNode.java etbCS/clientMode.java
	Note: Some input files use or override a deprecated API.
	Note: Recompile with -Xlint:deprecation for details.
	Note: Some input files use unchecked or unsafe operations.
	Note: Recompile with -Xlint:unchecked for details.
	$ ETB2 built successfully$
	```

	The text <span style="color:red">ETB2 has failed to build</span> signals problem with the building process. One possible problem could be the access right for the **build.sh** shell script file, and make sure that it is executable by running the command below.

	```console
	$ chmod -x build.sh
	```

3. Let us set up ETB2 by adding the alias command **alias etb2='java -cp .:$dependencies/commons-exec-1.3.jar:dependencies/commons-io-2.6.jar:dependencies/json-simple-2.1.2.jar:dependencies/org.json.jar etb/etbCS/etbNode'** in your **bash_profile**, **bashrc** or similar locations.

4. Now run the command below.

	```console
	$ etb2 -help
	```

	... and you will see the help menu for ETB2.

	```console
	Overview:  ETB 2.0 - Evidential Tool Bus (Linux 64-bit version)

	Usage:     etb2 [options] <inputs>

	Options:
	-help/-h          shows this help menue
	-init             initialises an etb node at a given location
	-node-info        displays details of the node, like its port, claims, workflows, local services and available remote servers/services
	-clean            removes available local services and remote servers from the server
	-uninit           deletes initialisation componenets of the node
	-set-port <int>   sets <int> as the port number of the server
	-set-repo <dir>   sets <dir> as the git repo used as working directory
	-query <term>     runs a query to get solutions for the given term
	-script <file>    executes a file with datalog workflow to get solutions for its queries
	-add-service      adds local service(s) to the server
	-rm-service       removes local service(s) from the node
	-add-server       adds remote server(s) whose services will avilable to the etb node
	-rm-server        removes remote servers
	-add-claim        adds claim(s) to the etb node
	-rm-claim         removes claim(s) from the etb node
	-update-claim     updates an outdated claim
	-upgrade-claim    upgrades an outdated claim
	-reconst-claim    reconstructs an outdated claim
	```

	##  Running ETB2

	In This section, we see how an ETB node can be initialised on a local machine and used for integrated and continuous verification tasks to generate verification claims supported by evidences.

	### Node initialisation

	If we run the command 'etb2 -node-info' right after building and setting up ETB2, we get a notification that no ETB node is yet intialised on the our machine.   

	```console
	$ etb2 -node-info
	[error] no ETB node at this location (use -init to initialise an ETB node)
	```

	The first step in using ETB2 is to set up an ETB node on a given machine. This is done by running the command 'etb2 -init'. As an ETB node is uniquely identified by its IP and port number, the first thing ETB2 prompts for is a port number.

	```console
	$ etb2 -init
	--> provide port :
	```

	Once a valid port number is provided, ETB2 further prompts for a valid git repository that will be used as a workspace by the ETB node to store claims, files and evidences involved in any verification effort the node has taken part in.   

	```console
	$ etb2 -init
	--> provide port : 3435
	--> provide git repo : TEMP
	ETB node initialised (use -h to see more options to update the node)
	```

	Once valid inputs for these two parameters are given, ETB2 initialises an ETB node on the machine.
	If we run the command 'etb2 -node-info' now, we get see similar information like the one below about the ETB2 node we have just initialised.

	```console
	$ etb2 -node-info
	hostIP : 192.168.0.32
	port : 3435
	git repo path : ETB2/TEMP
	==> total number of claims: 0
	==> total number of workflows: 0
	==> total number of local services: 0
	==> total number of servers: 0
	```

	You can see that the port number and the git repo provided above during the initialisation step in the node info. The hostIP parameter is the host machine's IP address, which is automatically read by ETB2 during the node initialisation step.

	Note that the number of claims, workflows, local services and servers are all set to zero. This is not surprising as the node is just created and we have not yet added anyone of these components.

	### Adding local services/tools

	Any tool or service installed in the local machine, i.e., the machine in which the ETB node under consideration is running, can be made available to the node. This is done by adding the service using the command 'etb2 -add-service', which starts an interactive command prompt asking for four inputs.

	The first input is *service name* - a unique identifier for the service.

	```console
	$ etb2 -add-service
	--> service name :
	```

	Once we provide the service name, ETB2 asks for the *service signature*, which is a list of argument types for a given service. Currently, we support four argument types in ETB2. These are *string*, *file*, *string_list* and *file_list*.

	Syntactically, a service signature is a list of such types separated by (at least one) space. For example, the signature '*file file_list string*' specifies that a given service has *file* as type for its first argument, *list of files* as type for its second argument, and *string* as type for its third argument.

	```console
	$ etb2 -add-service
	--> service name : cbmcMC
	--> service signature : file file_list string
	--> number of invocation modes :
	```

	Before considering the next parameters, let us see what a *service mode* means.
	A mode for a given service defines which of the service arguments are given as inputs to the service during its invocation (before its execution) and which are outputs that are expected to be produced by the service at the end of its execution.

	Inspired by prolog predicate input/output notation, we use **+** for input arguments and **-** for output arguments. For example, the mode **++-** for a service with three arguments specifies that the first two arguments are inputs to the service and the last argument is the output of the service. In ETB2, a service can be invoked in different modes at different times.

	The third parameter prompts for the number of invocation modes for the service being added to an ETB2 node.  

	```console
	$ etb2 -add-service
	--> service name : cbmcMC
	--> service signature : file file_list string
	--> number of invocation modes : 2
	--> set of modes :
	```

	Once the number of modes is given, the service addition procedure further prompts for the set of invocation modes. An example mode set is **{++-, +++}**.

	```console
	$ etb2 -add-service
	--> service name : cbmcMC
	--> service signature : file file_list string
	--> number of invocation modes : 2
	--> set of modes : {++-, +++}
	=> tool added successfully
	```

	If a service is no more required by an ETB node, it can be removed interactively by running the command 'etb2 -rm-service'

	### Adding workflows

	ETB2 uses Datalog as a scripting language to define verification workflows that employ local and remote services to do integrated verification.

	A workflow should be added to an ETB2 node to automate its execution. This is done by adding the service using the command *'etb2 -add-workflow'*, which starts an interactive command prompt asking for three inputs.

	The first input is *workflow name*, which is a unique identifier for the workflow.

	```console
	$ etb2 -add-workflow
	--> workflow name : codeReview
	```
	After reading the workflow name (*codeReview* above), ETB2 asks for the *workflow script path*.
	A workflow script is a Datalog program that defines the logic of a given verification process.

	```console
	$ etb2 -add-workflow
	--> workflow name : codeReview
	--> workflow script path : TEMP/workflowCR.dl
	--> query list (see help menu for syntax) :
	```

	Finally, ETB2 prompts for *query list*, which is a list of queries (syntactically Datalog literals) that the node can be asked to search for a satisfying claim possibly with supporting evidence composed from tools employed in the claim validation process.

	Syntactically, a *query list* is a set of *query specifications*, where each *query specifications* a tuple of the following three components separated by semicolons.
	- **name**: unique identifying string for the workflow
	- **signature**: list of data types in a bracket and separated by commas.
	An example *query signature* is *(file, file_list, string)*.
	- **mode**: invocation mode of the query. An example mode can be '*++-*'.

	An example *query list* is *{<twoStepCR; (file,file,file,file,file,file); +++++->, <threeStepCR; (file,file,file,file,file,file); +++++->}*.

	```console
	$ etb2 -add-workflow
	--> workflow name : codeReview
	--> workflow script path : TEMP/workflowCR.dl
	--> query list (see help menu for syntax) : {<twoStepCR; (file,file,file,file,file,file); +++++->, <threeStepCR; (file,file,file,file,file,file); +++++->}
	=> workflow added successfully
	```

	### Adding remote servers

	### Adding claims
