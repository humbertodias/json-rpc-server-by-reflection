package rpc.service;

import java.time.Instant;

public class Misc {

    public double pi(){
        return Math.PI;
    }

    public String now(){
        return Instant.now().toString();
    }

}
