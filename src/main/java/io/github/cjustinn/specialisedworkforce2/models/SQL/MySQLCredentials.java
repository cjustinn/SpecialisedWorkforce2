package io.github.cjustinn.specialisedworkforce2.models.SQL;

public class MySQLCredentials {
    final public String host;
    final public String port;
    final public String database;
    final public String username;
    final public String password;

    public MySQLCredentials(String host, String port, String database, String username, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    public String getConnectionString() {
        return String.format(
                "jdbc:mysql://%s:%s/%s",
                this.host,
                this.port,
                this.database
        );
    }
}
