databaseChangeLog = {

    changeSet(author: "marko (generated)", id: "1471011648457-1") {
        createTable(tableName: "micro_service_api_key") {
            column(autoIncrement: "true", name: "id", type: "BIGINT") {
                constraints(primaryKey: "true", primaryKeyName: "micro_service_api_keyPK")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "micro_service", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "secret_key", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(defaultValueBoolean: "true", name: "valid", type: "BOOLEAN") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "marcus (generated)", id: "1471947455462-1") {
        addColumn(tableName: "job") {
            column(name: "deleted", type: "BOOLEAN") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "marcus (generated)", id: "1472133595073-1") {
        addColumn(tableName: "job_group") {
            column(name: "persist_detail_data", type: "bit") {
                constraints(nullable: "false")
            }
        }
    }
}
