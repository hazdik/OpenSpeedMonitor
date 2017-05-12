databaseChangeLog = {
    changeSet(author: "dkl", id: "1494576018-1") {
        addColumn(tableName: "job") {
            column(name: "option_domelement"       , type: "varchar(255)")
            column(name: "option_is_private"       , type: "BOOLEAN")      {constraints(nullable: "false")}
            column(name: "option_connections"      , type: "integer")
            column(name: "option_block"            , type: "varchar(255)")
            column(name: "option_auth_type"        , type: "BOOLEAN")      {constraints(nullable: "false")}
            column(name: "option_f"                , type: "varchar(255)")
            column(name: "option_r"                , type: "varchar(255)")
            column(name: "option_notify"           , type: "varchar(255)")
            column(name: "option_pingback"         , type: "varchar(255)")
            column(name: "option_noopt"            , type: "BOOLEAN")      {constraints(nullable: "false")}
            column(name: "option_noimages"         , type: "BOOLEAN")      {constraints(nullable: "false")}
            column(name: "option_noheaders"        , type: "BOOLEAN")      {constraints(nullable: "false")}
            column(name: "option_pngss"            , type: "BOOLEAN")      {constraints(nullable: "false")}
            column(name: "option_iq"               , type: "integer")
            column(name: "option_mobile"           , type: "BOOLEAN")      {constraints(nullable: "false")}
            column(name: "option_uastring"         , type: "varchar(255)")
            column(name: "option_width"            , type: "integer")
            column(name: "option_height"           , type: "integer")
            column(name: "option_browser_width"    , type: "integer")
            column(name: "option_browser_height"   , type: "integer")
            column(name: "option_dpr"              , type: "integer")
            column(name: "option_mv"               , type: "BOOLEAN")      {constraints(nullable: "false")}
            column(name: "option_median_metric"    , type: "varchar(255)")
            column(name: "option_cmdline"          , type: "varchar(255)")
            column(name: "option_htmlbody"         , type: "BOOLEAN")      {constraints(nullable: "false")}
            column(name: "option_tsview_id"        , type: "varchar(255)")
            column(name: "option_custom"           , type: "varchar(255)")
            column(name: "option_tester"           , type: "varchar(255)")
            column(name: "option_affinity"         , type: "varchar(255)")
            column(name: "option_timeline"         , type: "BOOLEAN")      {constraints(nullable: "false")}
            column(name: "option_timeline_stack"   , type: "integer")
            column(name: "option_mobile_device"    , type: "varchar(255)")
            column(name: "option_appendua"         , type: "varchar(255)")
            column(name: "option_lighthouse"       , type: "BOOLEAN")      {constraints(nullable: "false")}
            column(name: "option_type"             , type: "varchar(255)")
        }
    }
}
