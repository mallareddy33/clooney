package com.clooney.agent.spec;

public class Prompts {

    public static String buildSpecPrompt(String summaryJson) {
        return """You are an expert backend engineer.\n\nHere is a JSON summary of observed endpoints:\n\n""" 
                + summaryJson 
                + """\n\nReturn: \n===OPENAPI===\n<dummy>\n===SCHEMA_SQL===\n<dummy>\n===END===""" ;
    }

    public static String buildBackendPrompt(String openapiYaml, String schemaSql) {
        return """Generate Spring Boot backend code.\nOpenAPI:\n""" + openapiYaml +
                """\nSchema:\n""" + schemaSql +
                """\nReturn files in ===FILE:...=== format.""" ;
    }

    public static String buildTestsPrompt(String openapiYaml) {
        return """Generate JUnit tests using RestAssured.\nOpenAPI:\n""" + openapiYaml +
                """\nReturn files in ===FILE:...=== format.""" ;
    }
}
