package com.edi.service;

import it.sogei.domest.infomrnfp.Richiesta;
import it.sogei.ws.output.Risposta;

public class App {
    public static void main(String[] args) {
        System.out.println("EDI Service Application");

        try {
            // Initialize the MRN client
            // Replace with actual values
            String endpointUrl = "https://interoptest.adm.gov.it/InfoMRNFPWeb/services/InfoMRN";
            String p12CertificatePath = "certificates/max/AgenziaDoganeMonopoli.p12";
            String password = "4321ADM1234"; // Password for the certificate

            System.out.println("Initializing MRN client with endpoint: " + endpointUrl);
            System.out.println("Using P12 certificate: " + p12CertificatePath);
            MrnInfo.initialize(endpointUrl, p12CertificatePath, password);

            // Use a realistic MRN number format (18 characters)
            // Format: YYITxxxxxxxxxxxxxxx (YY=year, IT=country code, x=alphanumeric)
            String mrn = "23IT12345678901234";
            System.out.println("Querying MRN: " + mrn);

            // Create the request
            Richiesta richiesta = MrnInfo.createRequest(mrn, null);
            System.out.println("Request created with service ID: " + richiesta.getServiceId());
            System.out.println("Request dichiarante: " + richiesta.getData().getDichiarante());

            // Process the request
            System.out.println("Sending request to server...");
            Risposta response = MrnInfo.getInstance().process(richiesta);
            System.out.println("Response received: " + response);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}