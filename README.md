## Information on Sonar Cloud, the Platform providing the important metrics.

What is **SonarQube**?

**SonarQube** is an open-source platform developed by SonarSource for continuous inspection of code quality to perform automatic reviews with static analysis of code to detect bugs and code smells on 29 programming languages. 

Why do we use **SonarQube**?

SonarQube offers reports on duplicated code, coding standards, unit tests, code coverage, code complexity, comments, bugs, and security recommendations. SonarQube provides automated analysis and integration with Maven, Ant, Gradle, MSBuild, and continuous integration tools.


**What is SonarCloud?**

SonarCloud is a cloud-based code analysis service designed to detect coding issues. It can be used to run an independent cloud-based SonarQube server to review code every time a commit happens on a repository.

How to set up SonarCloud to a Git repository?


When you first sign up for SonarCloud you have to choose which DevOps platform you want to connect to.

Your SonarCloud account is created and bound to your account on the DevOps platform chosen, i.e. GitHub, Bitbucket cloud, Gitlab etc.

At this point, you can import organizations from your DevOps platform to SonarCloud and then import repositories from those organizations. Each imported organization becomes a SonarCloud organization and each imported repository becomes a SonarCloud project. 

After selecting **Analyze New Project**, you will be presented with a step-by-step tutorial to install the SonarCloud application on GitHub. This allows SonarCloud to access your GitHub organization or personal account. You can select specific repositories to be connected to SonarCloud or just select all and can always change this setting later.

SonarCloud will suggest an Actions secret **NAME** and **KEY** for your SonarCloud organization. The key is unique across all organizations within SonarCloud. You can accept the suggestion or change it manually. The interface will prevent you from changing it to an already existing key.

The next step is to import the projects (that is, individual Git repositories) that you want to analyze from your GitHub organization into your newly created SonarCloud organization. A corresponding, one-to-one SonarCloud project will be created for each imported repository.

SonarCloud can automatically analyze your code simply by reading it from your GitHub repository, without the need to configure a CI-based analysis.

If automatic analysis is not recommended for your project, you will need to set up a CI-based analysis. This will be the case, for example, with projects that use **PL/SQL, TSQL or Objective-C**. Here, the analysis process happens in the build process instead of SonarCloud. Just select the type of code the project is based on and SonarCloud will guide you through the steps to set it up.

Once this is done, you can view the results of your first analysis. Based on your choice of **‘New Code’**, you will see a new analysis by duration or each new commit.


A **quality gate** is an indicator that tells you whether your code meets the minimum level of quality required for your project. It consists of a set of conditions that are applied to the results of each analysis. If the analysis results meet or exceed the quality gate conditions then it shows a **Passed** status otherwise, it shows a **Failed** status.

What kind of information does the analysis give us?


**Issues** are the most important thing to work on code. The quality gates are failed on introduction of new issues in code, if not configured differently. It gives an overview of the issues so they can be reviewed/resolved.

SonarCloud provides us with **measured ratings**. These are crucial to the calculation of quality gates and should be carefully reviewed before deployment of code. The ratings are given as **Maintainability, Reliability, Security, Security Hotspots and Coverage**. 

It also gives us information about other metrics. Some of these **are Complexity, New Violations, Bugs, NCLOC (lines of code) and Vulnerabilities**.

Webhooks and why we use them


**Webhooks** notify external services when a project analysis is complete. An **HTTP POST** request including a **JSON** payload is sent to each URL. Depending on the type of request given, certain aspects of the projects like **the file types, commit hash or even the metrics** can be included in the payload. This makes it easy for automating the process of getting information from the analyses.

We can also specify which metrics have to be sent by the SonarCloud Webhook by changing the URL specified. This is done by:

https://sonarcloud.io/api/measures/component?component=**<Project\_Key>**&metricKeys=**<Metrics\_Required>**


The **project key** can be acquired from the profile page. This is **unique to every project** so that the webhook knows which project to refer to.

**Metrics\_Required** can be specified in csv format as follows: **complexity,ncloc,reliability\_rating** .

We can also get project specific information like the git commit hash. This is also done by tweaking the Webhook URL. To get the commit hash, we can use the following webhook:

https://sonarcloud.io/api/project\_analyses/search?project=<**Project\_Key**>

Other attributes of the project can be acquired in a similar manner. Refer to the official SonarCloud API Documentation for further command information.

What can we do with the Payload

The Webhook provides us with relevant information in JSON format. The JSON format makes it easy to manipulate and extract information.

For example, the response block from the above provided webhook URL might look like:

```
{

`    `"metrics": {

`        `"component": {

`            `"id": “<Project\_ID>”,

`            `"key": "<Project\_Key>",

`            `"name": "<Project\_Name>",

`            `"qualifier": "TRK",

`            `"measures": [

`                `{

`                    `"metric": "complexity",

`                    `"value": "22"

`                `},

`                `{

`                    `"metric": "minor\_violations",

`                    `"value": "1",

`                    `"bestValue": false

`                `},

`		`]

`         `}

`     `}

}
```

As we can see, this can be stored in a file like **sample.json** and manipulated using any programming language like **Python, Java, etc**.




## **Implementing the Bug Frequency and Code Review Server**
- The Bug Frequency Server would target the historical data of the company. By using data on filenames and the priorities it has appeared in the past it can give us the severity level as to how critical a change in that file could be.

**Major Steps in creating this server:**

1. A Database server with all the filenames and the priorities it appeared in must be setup.
   1. This database must be updated frequently to have latest data.
1. Once the data for each filename is present, we have a way of accessing the file attributes when the Diff containing the requested filename is received.
1. Now, a weight-based severity calculator is installed in this server that takes the priorities and the number of times the filename has appeared in those distinct priorities to assign a severity index.
1. This is then Fed to a Random Forest ML model that learns how the filenames and the priorities they appeared at affect the severity of the file.
1. This severity then sent along with the Code Review Severity to the Severity Calculator Python Server.
1. Note that the severity calculated here is only for the filenames and their historical context. It is supposed to be used as an influencing factor in the main severity calculator sever.
1. After this a code review utility is added that uses the given code diff and a prompt template to ask the LLM to review that code and give the severity of the code Diff.

As shown in the HLD, this server gets the Code Diff from the Java Relay Server. Once it receives the Code Diff it performs the following tasks in parallel:

1. Uses the bug frequency module to:
   1. Extract the filenames edited in the code diff and search the database for the filename and gets the priorities they appeared at.
   1. Then feed this filename and the priorities to a pretrained model that then gives out a severity for that file.
1. Uses the LLM code review module to get a review of the code diff from the LLM.

Once it receives the LLM code review and the historical severity from the ML model it sends it back to the Java Relay Server.

Information about llm that reviews the code

What is LLM?

LLM stands for Large Language Model, a type of AI model trained on extensive text data to understand and generate human-like language. These models, like  GenAI, are versatile in natural language tasks, using transformer architectures to handle complex linguistic patterns. They are pivotal in applications from chatbots to translation, leveraging pre-training and fine-tuning to adapt to specific tasks and domains, making them essential in modern AI-driven text processing.



Here, we use LLM for reviewing our code to find the errors in code.

Files

- Index.html
- request.py
- request\_service.py

request.py opens the index.html file, which takes the input as a code diff and sends it to request\_services.py. This file contains the LLM (GenAI-API) model, which reviews the code diff and provides information about errors, as well as suggestions to help make corrective decisions.

Process

**INPUT**

Take input as code diff


Sends the input in backend

**What is code diff?**

`   `A "code diff" (short for "difference") is a representation of changes made to a codebase. It shows the differences between two versions of a file or a set of files. This is commonly used in version control systems like Git to track changes, review code, and collaborate with others.

A typical diff output includes:

1. **Removed lines**: Lines that were present in the old version but are not in the new version. These are usually marked with a minus (-) sign and often highlighted in red.
1. **Added lines**: Lines that are present in the new version but were not in the old version. These are usually marked with a plus (+) sign and often highlighted in green.
1. **Unchanged lines**: Lines that are present in both versions and are often shown for context. These lines are typically not marked or highlighted.

Diffs can be viewed using various tools and interfaces, such as command-line tools (git diff for Git), integrated development environments (IDEs), and web interfaces (like GitHub or GitLab).

**Example of diff**

**PROCESS**

In request\_service.py, the get\_completion function is structured to handle input validation, construct a specific prompt for a code review task, interact with a generative AI model to perform the review and severity assessment, and handle potential errors gracefully by providing informative error messages.

This function is useful in scenarios where automated code review and assessment based on AI-generated insights are desired, potentially aiding in identifying and prioritizing software bugs or flaws based on their perceived severity.

**What is the process of code review?**

**1. Sending a Request**

- **Input Data**: We can provide code diff as input.
- **Request Format**: The request is typically sent in a structured format, such as JSON, over HTTP or HTTPS.

**2. Processing the Request**

- **Model Invocation**: The API server receives the request and passes the input data to the generative model hosted on the server.
- **Generation Process**: The model processes the input and generates the output based on its training.

**3. Receiving the Response**

- **Generated Content**: The API returns the generated content as a **response** in text format


**OUTPUT**

Once we provide the diff, it will be able to do:

- **A detailed code review:** Identifying potential issues like bugs, security vulnerabilities, code style violations, and best practice violations.
- **Severity rating:** It will assign a severity rating on a scale of 1 to 10, where 1 represents a minor issue and 10 represents a critical issue.

**Recommendations for improvement:** It will suggest ways to address the identified issues and improve the code quality.


## **Implementing the Java Relay Server**

**Introduction**

The Java Relay Server facilitates seamless communication and integration between services in the software development lifecycle. It manages code differentials, conducts automated code reviews using an LLM (Large Language Model), and integrates bug severity data for enhanced code quality assessment.

**Steps for workflow of this server:**

- Once the Metrics is received from the Sonar Cloud, the java relay server fetches the commit hash related with those metrics.
- After receiving the metrics and the commit has related to that commit, it first fetches the commit diff from bitbucket.
- It then extracts the filenames from that commit diff to retrieve the cyclomatic complexities of those files from sonar.
- Then it sends the diff to the python server to extract the historical severity and the code review severity from the LLM.
- Once it has all the necessary metrics it performs calculations to get the severity from sonar, the historical severity, and the code review.
- Then by giving specified importance to the three calculated severities it calculates a master severity.

**NOTE: The react frontend server provides an interface to view all the results and metrics at one place.**


## SEVERITY Calculator (Installed within the Java Relay Server)


Checking for **code severity** is essential for maintaining the overall health and performance of a software system. High-severity issues often represent significant threats such as **security vulnerabilities, potential system crashes, or data corruption**, which can have catastrophic consequences if not promptly addressed. 

By assessing the severity of code issues, development teams can prioritize their efforts, ensuring that the most critical problems are resolved first to mitigate major risks. This prioritization is crucial for effective resource allocation, as **high-severity issues** may require immediate attention from more experienced developers, while lower-severity issues can be scheduled for future sprints or handled by junior team members. .

Ensuring that code meets high standards of quality and security can help organizations stay compliant with these regulations and avoid potential legal repercussions. 

Additionally**, early detection and correction of severe issues** can be more cost-effective, as the impact and expense of fixing problems escalate significantly once the software is deployed. Regularly checking for code severity fosters a culture of continuous improvement, leading to a more robust, maintainable, and scalable codebase over time. 

This practice not only supports the immediate needs of the software but also contributes to its long-term success and sustainability.

>How do we check for code severity?

Checking for code severity involves a comprehensive approach that combines automated tools and manual processes. Automated static code analysis tools like **SonarQube, ESLint, and FindBugs** scan the code for potential issues without executing it, identifying **syntax errors, code smells, and security vulnerabilities**, often providing severity ratings. Dynamic analysis tools such as **Valgrind and Coverity** analyze the behavior of the code during execution to detect runtime issues. 

Additionally, monitoring tools like **New Relic** and log analysis tools like **ELK Stack** help detect anomalies and errors that indicate severe problems. Establishing user feedback channels and a triage process prioritizes reported issues based on their severity. Developing a custom severity rating system tailored to the project's needs and maintaining **clear documentation** ensures all team members are aligned in assessing code severity effectively. 

This holistic approach ensures critical problems are addressed promptly, maintaining the software's integrity and reliability.



> What tools are used in our severity calculator?

The severity calculator operates on **Java based code**. This calculator helps request data from the dedicated **SonarQube server** and the **python-based bug frequency server which also houses the LLM API receiver**.

As mentioned above, one of the most important steps of checking code severity is static code analysis. This is done on the server using **SonarQube**. SonarQube gives us important metrics required to compute code severity and alert status. 

We also use a **Large Language Model** (gemini-1.5-flash in this case) to give us a comprehensive code review for the code diff we provide. These components give us a directive about the current code changes and necessary alterations to be done. 

Alongside these, we use historical data of priority assignment of all files in our repository. This gives us an understanding of the severity of each file and the impact that changes to a certain file can bring to the code. 

To assess the historical data, we use the **Bug Frequency server** to extract data weekly from JIRA with priority assigned as the main pivot for severity. This gives us a fairly accurate sense of whether making changes to a file in the repository will cause undesirable results.

What kinds of inputs do we take from each of the tools used and how do we process them?

As the most important part of the calculator, we will first consider the inputs received from SonarQube. Once a commit is made to the repository, SonarQube automatically runs an analysis through the files **“sonar-project.properties”** and **“build.yml”**. This analysis gives much information in the form of metrics. Some of them include **complexity, ncloc (non-commented lines of code), security rating, code smells, etc**. 

The most important of all the metrics is the **“alert status”** metric which gives us an idea whether the code has passed the quality gates or not. It is highly recommended to review and improve code before deployment if the **“alert status” metric returns “ERROR”**.

The metrics that pertain to the newest code changes are fetched by the Java relay server and processed using a weighted mean system to give an accurate representation of the severity caused by said changes. This is done by introducing **equalizers** that give a measure of importance to a particular metric based on existing **maximum values**. The following method is used to then calculate the severity given by the metrics:

> Sonar Severity= 
$$
\left( \frac{\sum_{i=1}^n (eq_i \times v_i)}{\sum_{i=1}^n (eq_i \times m_i)} \right) \times 100
$$


Here, vi **is the value,** mi **is the maximum value and** eqi **is the equalizer for a said metric**.



The second input is the code review we obtain from the **Large Language Model (LLM)**. This gives us two inputs, a **numerical value depicting severity** and a block of text giving **review and recommended changes**. The review is directly shown on the front-end whereas the numerical value is stored as **LLM Severity**.

The third input is recieved from the Bug frequency server and this is stored as Bug Freq Severity.

The final Master Severity is calculated as a simply weighted mean as depicted below:

Master Severity=0.6×Sonar Severity+0.3×Bug Freq Severity+0.1×LLM Severity

(Sonar Severity = 60%, Bug Frequency Severity = 30% and LLM Severity = 10%)

This value of Master Severity lies between 0 and 100 accurately showing the severity caused by the latest commit to the repository.


![High Level Design](hld.png)
