/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 *     Abdsandryk Souza - adding default currency and fromCursor
 ******************************************************************************/
package tw.tib.financisto.model;

import java.math.BigDecimal;
import java.text.Format;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import tw.tib.financisto.utils.CurrencyCache;

@Entity
@Table(name = "currency")
public class Currency extends MyEntity {

	public static final Currency EMPTY = new Currency();
	
	static {
        EMPTY.id = 0;
        EMPTY.name = "";
        EMPTY.title = "Default";
		EMPTY.symbol = "";
        EMPTY.symbolFormat = SymbolFormat.RS;
		EMPTY.decimals = 2;
        EMPTY.decimalSeparator = "'.'";
        EMPTY.groupSeparator = "','";
	}

	@Column(name = "name")
	public String name;

	@Column(name = "symbol")
	public String symbol;

    @Column(name = "symbol_format")
    public SymbolFormat symbolFormat = SymbolFormat.RS;

	@Column(name = "number_format")
	public String numberFormat;

	@Column(name = "is_default")
	public boolean isDefault;

	@Column(name = "update_exchange_rate")
	public boolean updateExchangeRate;

	@Column(name = "decimals")
	public int decimals = 2;

	@Column(name = "decimal_separator")
	public String decimalSeparator;

	@Column(name = "group_separator")
	public String groupSeparator;

	@Column(name = "trading_currency_id")
	public long tradingCurrencyId;

    @Transient
	private volatile Format format;

	@Transient
	private volatile long divisor = 0;

	public Format getFormat() {
		Format f = format;
		if (f == null) {
			f = CurrencyCache.createCurrencyFormat(this);
			format = f;
		}
		return f;
	}

	/**
	 * The number of digits to the right of the decimal point in actual stored value.
	 *
	 * To compatible with existing data, currencies with decimals less than 2 places
	 * are still treated as having 2 decimal points in stored value.
	 */
	public int getScale() {
		return decimals < 3 ? 2 : decimals;
	}

	/**
	 * Divide the amount with divisor to get the actual value
	 * @return the divisor
	 */
	public long getDivisor() {
		if (divisor == 0) {
			divisor = new BigDecimal(1).movePointRight(getScale()).longValue();
		}
		return divisor;
	}
	
	public static Currency defaultCurrency() {
		Currency c = new Currency();
		c.id = 2;
		c.name = "USD";
		c.title = "American Dollar";
		c.symbol = "$";
		c.decimals = 2;
		return c;
	}

	@Override
	public String toString() {
		return name;
	}

	public String dump() {
		return "Currency{" +
				"id=" + id +
				", name='" + name + '\'' +
				", symbol='" + symbol + '\'' +
				'}';
	}
}
