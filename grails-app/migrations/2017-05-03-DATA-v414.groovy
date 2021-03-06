databaseChangeLog = {
    changeSet(author: 'sbr', id: '1493813880000-1') {
        sql('''UPDATE userspecific_dashboard_base SET
               `from_date`=CONCAT(YEAR(from_date), '-', MONTH(from_date), '-', DAY(from_date), ' 00:00:00'),
               `to_date`=CONCAT(YEAR(to_date), '-', MONTH(to_date), '-', DAY(to_date), ' 23:59:59.999999');''')
    }
    changeSet(author: 'sbr', id: '1493813880000-2') {
        sql('''UPDATE userspecific_dashboard_base SET 
               `from_date`=CONCAT(YEAR(from_date), '-', MONTH(from_date), '-', DAY(from_date), ' ', from_hour)
               WHERE set_from_hour=1''')
    }
    changeSet(author: 'sbr', id: '1493813880000-3') {
        sql('''UPDATE userspecific_dashboard_base SET
            `to_date`=CONCAT(YEAR(to_date), '-', MONTH(to_date), '-', DAY(to_date), ' ', to_hour)
            WHERE set_to_hour=1;''')
    }
}
