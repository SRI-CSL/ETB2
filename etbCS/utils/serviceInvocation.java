package etb.etbCS.utils;

import java.util.*;
import java.util.HashMap;
import etb.etbDL.utils.Expr;
import etb.wrappers.*;
import etb.etbDL.output.*;
import etb.etbCS.clientMode;
import etb.etbCS.etbNode;

public class serviceInvocation {

    Expr query;
    Expr result; //TODO: for more than one results per query
    String evidence = null;
    boolean local = false;
    
    public serviceInvocation(Expr query) {
        this.query = query;
    }
    
    public void process(etbNode node) {
        servicePackage servicePack = node.getServicePack();
        serversPackage serversPack = node.getServersPack();
        String serviceID = query.getPredicate();
        if (servicePack.containsService(serviceID)) {
            System.out.println("\t -> query processing as a local service");
            System.out.println(servicePack.get(serviceID).toString());
            invoke(query, servicePack.get(serviceID).getSignature(), node.getRepoDirPath());
            local = true;
        }
        else {
            //getting all servers providing the service requested in the query
            List<serverSpec> serviceProviderServers = Arrays.asList(serversPack.getServers().values().stream().filter(eachServerSpec -> eachServerSpec.getServices().contains(serviceID)).toArray(serverSpec[]::new));
            //TODO: grabbing the service from the available servers
            Iterator<serverSpec> serverIter = serviceProviderServers.iterator();
            while (serverIter.hasNext()) {
                serverSpec providerServer = serverIter.next();
                System.out.println("\t -> processing as a remote service");
                System.out.println("\t\t -> server spec " + providerServer);
                clientMode cm = new clientMode(providerServer.getAddress(), providerServer.getPort());
                if (cm.isConnected()) {
                    this.result = cm.remoteServiceExecution(query, node.getRepoDirPath());
                    this.evidence = cm.getEvidence();
                    //TODO: special tactic/heuristic for more servers per service?
                    break;
                }
            }
        }
    }

    public Expr getResult(){
        return result;
    }
    
    public String getEvidence() {
        return evidence;
    }
   
    private void invoke(Expr query, String signature, String repoDirPath) {
        try {
            Class<?> wrapperClass = Class.forName("etb.wrappers." + query.getPredicate() + "WRP");
            Object wrapper = wrapperClass.newInstance();
            genericWRP genWRP = (genericWRP) wrapper;
            genWRP.invoke(query.getMode(), query.getTerms(), repoDirPath + "/TEMP/" + query.getPredicate());
            this.result = new Expr(query.getPredicate(), genWRP.getOutParams(), signature, query.getMode());
            this.evidence = genWRP.getEvidence();

        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            System.out.println("\u001B[31mmissing service wrapper\u001B[30m please check '" + query.getPredicate()+ "' service)");
            e.printStackTrace();
        }
    }
    
    public boolean isLocal() {
        return local;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("--> query: " + query);
        if (result.equals(null)){
            sb.append("--> result: null (\u001B[31m [warning]\u001B[30m service could not be invoked)");
        }
        else {
            sb.append("--> result: " + result);
        }
        //sb.append("--> bindings: " + OutputUtils.bindingsToString(bindings));
        if (evidence.equals(null)) {
            sb.append("--> evidence: null (\u001B[31m [warning]\u001B[30m please check the wrapper)");
        }
        else {
            sb.append("--> evidence: " + evidence);
        }
        return sb.toString();
    }
    
}

