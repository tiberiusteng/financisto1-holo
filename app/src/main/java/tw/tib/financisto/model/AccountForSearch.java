package tw.tib.financisto.model;

import static tw.tib.financisto.db.DatabaseHelper.V_ACCOUNT;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = V_ACCOUNT)
public class AccountForSearch extends Account {
    @Column(name = "currency_name")
    public String currencyName;
}
