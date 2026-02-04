# AIQuerySelect

AIQuerySelect is a secure service that allows an AI to execute SQL `SELECT` queries against a database. It is designed to be used as a tool in a larger AI application, providing a safe and controlled way for the AI to access and retrieve data.

## Features

- **Secure Query Execution:** Only `SELECT` and `WITH` statements are allowed. All other DML/DDL/DCL statements are blocked to prevent unauthorized modifications to the database.
- **SQL Injection Protection:** The service validates and cleans all incoming queries to prevent SQL injection attacks.
- **Schema Introspection:** Provides endpoints to retrieve detailed information about the database schema, including tables, columns, constraints, and foreign keys.
- **Row Limiting:** Automatically applies a row limit to all queries to prevent excessive data retrieval.
- **Spring AI Integration:** Built as a Spring AI "tool" that can be easily integrated into a larger AI system.

## Technologies

- Java 21
- Spring Boot
- Spring AI
- Spring JDBC
- Oracle Database

## How it Works

1.  **Schema Discovery:** The AI can use the `/schema` endpoints to discover the database schema. This allows the AI to understand the available tables and their relationships.
2.  **Query Generation:** Based on the schema information, the AI generates a SQL `SELECT` query to retrieve the desired data.
3.  **Query Execution:** The AI sends the query to the `/query` endpoint.
4.  **Validation:** The `QueryValidator` service intercepts the query and performs a series of security checks.
5.  **Execution:** If the query is valid, the `QueryService` executes it against the database.
6.  **Results:** The query results are returned to the AI in a structured format.

## Setup and Running

### Building the application

First, build the application using Gradle:

```bash
./gradlew build
```

### Running from the Command Line

You can run the application from the command line, passing the database credentials as arguments:

```bash
java -jar build/libs/AIQuerySelect-0.0.1-SNAPSHOT.jar \
  --DB_URL=jdbc:oracle:thin:@your_db_host:1521/your_db_service \
  --DB_USERNAME=your_username \
  --DB_PASSWORD=your_password
```

### Running with Claude Desktop App

To use this service with the Claude Desktop App, you need to configure it in your `settings.local.json` file. This file is typically located in the `.claude` directory in your user home directory.

Add the following configuration to your `mcpServers`:

```json
{
  "mcpServers": {
    "my-oracle-query-server": {
      "command": "java",
      "args": [
        "-jar",
        "C:/your_project_dest/build/libs/AIQuerySelect-0.0.1-SNAPSHOT.jar",
        "--DB_URL=jdbc:oracle:thin:@your_db_host:1521/your_db_service",
        "--DB_USERNAME=your_username",
        "--DB_PASSWORD=your_password"
      ]
    }
  }
}
```

Replace the placeholder values for `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` with your actual database credentials.

---

# AIQuerySelect (Korean)

AIQuerySelect는 AI가 데이터베이스에 대해 SQL `SELECT` 쿼리를 실행할 수 있도록 하는 보안 서비스입니다. 더 큰 AI 애플리케이션에서 도구로 사용되도록 설계되었으며, AI가 데이터에 안전하고 통제된 방식으로 액세스하고 검색할 수 있는 방법을 제공합니다.

## 주요 기능

- **보안 쿼리 실행:** `SELECT` 및 `WITH` 문만 허용됩니다. 다른 모든 DML/DDL/DCL 문은 데이터베이스에 대한 무단 수정을 방지하기 위해 차단됩니다.
- **SQL 인젝션 방어:** SQL 인젝션 공격을 방지하기 위해 들어오는 모든 쿼리를 확인하고 정리합니다.
- **스키마 인트로스펙션:** 테이블, 열, 제약 조건 및 외래 키를 포함한 데이터베이스 스키마에 대한 자세한 정보를 검색하는 엔드포인트를 제공합니다.
- **행 제한:** 과도한 데이터 검색을 방지하기 위해 모든 쿼리에 자동으로 행 제한을 적용합니다.
- **Spring AI 통합:** 더 큰 AI 시스템에 쉽게 통합할 수 있는 Spring AI "도구"로 구축되었습니다.

## 기술 스택

- Java 21
- Spring Boot
- Spring AI
- Spring JDBC
- Oracle Database

## 작동 방식

1.  **스키마 검색:** AI는 `/schema` 엔드포인트를 사용하여 데이터베이스 스키마를 검색할 수 있습니다. 이를 통해 AI는 사용 가능한 테이블과 그 관계를 이해할 수 있습니다.
2.  **쿼리 생성:** 스키마 정보를 기반으로 AI는 원하는 데이터를 검색하기 위한 SQL `SELECT` 쿼리를 생성합니다.
3.  **쿼리 실행:** AI는 쿼리를 `/query` 엔드포인트로 보냅니다.
4.  **유효성 검사:** `QueryValidator` 서비스가 쿼리를 가로채 일련의 보안 검사를 수행합니다.
5.  **실행:** 쿼리가 유효하면 `QueryService`가 데이터베이스에 대해 쿼리를 실행합니다.
6.  **결과:** 쿼리 결과는 구조화된 형식으로 AI에 반환됩니다.

## 설정 및 실행

### 애플리케이션 빌드

먼저 Gradle을 사용하여 애플리케이션을 빌드합니다.

```bash
./gradlew build
```

### 명령줄에서 실행

명령줄에서 애플리케이션을 실행하고 데이터베이스 자격 증명을 인수로 전달할 수 있습니다.

```bash
java -jar build/libs/AIQuerySelect-0.0.1-SNAPSHOT.jar \
  --DB_URL=jdbc:oracle:thin:@your_db_host:1521/your_db_service \
  --DB_USERNAME=your_username \
  --DB_PASSWORD=your_password
```

### Claude 데스크톱 앱으로 실행

이 서비스를 Claude 데스크톱 앱과 함께 사용하려면 `settings.local.json` 파일에서 구성해야 합니다. 이 파일은 일반적으로 사용자 홈 디렉토리의 `.claude` 디렉토리에 있습니다.

`mcpServers`에 다음 구성을 추가합니다.

```json
{
  "mcpServers": {
    "my-oracle-query-server": {
      "command": "java",
      "args": [
        "-jar",
        "C:/your_project_dest/build/libs/AIQuerySelect-0.0.1-SNAPSHOT.jar",
        "--DB_URL=jdbc:oracle:thin:@your_db_host:1521/your_db_service",
        "--DB_USERNAME=your_username",
        "--DB_PASSWORD=your_password"
      ]
    }
  }
}
```
}
```

`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`의 자리 표시자 값을 실제 데이터베이스 자격 증명으로 바꾸십시오.