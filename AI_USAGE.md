# AI usage guide for this repository

## AI tools used

- GitHub Copilot
- Claude

## What they were used for

- Claude recommended the Spel for the template engine in this project
- Claude helped me to analyze one exception I met during the development,
- GitHub Copilot helped me quicky initialize the Spring Boot project and generated some basic code for the rule engine.
- GitHub Copilot helped me to implement the unit tests with my given test cases.
- GitHub Copilot generated the AGENT.md file for this repository.

## One suggestion accepted
Calude helped me to indicate the root cause of the following exception:

```code
L1008E: Property or field 'ONLINE' cannot be found on object of type 'com.gerard.aml.domain.TransactionScreenRequest' - maybe not public or not valid?
```

It was caused by 

```code
{
"name": "SUSPICIOUS_ONLINE_TRANSACTION",
"expression": "channel == 'ONLINE' && amount > 5000 && originCountry != destinationCountry"
}

```

the Spel parser thought ONLINE was a property of the TransactionScreenRequest class.
It helped me quickly fix the issue.



## One suggestion rejected 

I asked Github Copilot to give me some suggestion on how to improve the performance of the rule engine, it suggested me to cache the compiled Spel expressions, but I rejected this suggestion.

I did't want to build another endpoint to clear the cache and reload the rule file, but I though everytime I update the rule file it should take effect 
immediately, so I decided to compile the Spel expression every time I evaluate a transaction request.
