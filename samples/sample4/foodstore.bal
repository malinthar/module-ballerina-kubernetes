import ballerina/http;
import ballerinax/kubernetes;


@kubernetes:Ingress {
    hostname:"pizza.com",
    path:"/pizzastore",
    targetPath:"/"
}
@kubernetes:Service{}
endpoint http:ServiceEndpoint pizzaEP {
    port:9099
};

@kubernetes:Deployment {
    name:"foodstore",
    replicas:3,
    labels:{"location":"SL","city":"COLOMBO"},
    enableLiveness:"enable",
    livenessPort:9099
}

@http:ServiceConfig {
    basePath:"/pizza"
}
service<http:Service> PizzaAPI bind pizzaEP{
    @http:ResourceConfig {
        methods:["GET"],
        path:"/menu"
    }
    getPizzaMenu (endpoint outboundEP, http:Request req) {
        http:Response response = {};
        response.setStringPayload("Pizza menu \n");
        _ = outboundEP -> respond(response);
    }
}



@kubernetes:Ingress {
    hostname:"burger.com",
    path:"/",
    targetPath:"/burger"
}
@kubernetes:Service{}
endpoint http:ServiceEndpoint burgerEP {
    port:9096
};
@http:ServiceConfig {
    basePath:"/burger"
}
service<http:Service> BurgerAPI bind burgerEP {
    @http:ResourceConfig {
        methods:["GET"],
        path:"/menu"
    }
    getBurgerMenu (endpoint outboundEP, http:Request req) {
        http:Response response = {};
        response.setStringPayload("Burger menu \n");
        _ = outboundEP -> respond(response);
    }
}
