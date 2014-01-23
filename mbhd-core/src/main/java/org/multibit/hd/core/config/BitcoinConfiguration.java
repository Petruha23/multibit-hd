package org.multibit.hd.core.config;

/**
 * <p>Configuration to provide the following to application:</p>
 * <ul>
 * <li>Configuration of Bitcoin related items</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class BitcoinConfiguration {

  private String bitcoinSymbol = "ICON";
  private String exchangeClassName="";
  private String exchangeName="Mt Gox";

  /**
   * @return The Bitcoin symbol to use (compatible with BitcoinSymbol)
   */
  public String getBitcoinSymbol() {
    return bitcoinSymbol;
  }

  public void setBitcoinSymbol(String bitcoinSymbol) {
    this.bitcoinSymbol = bitcoinSymbol;
  }

  /**
   * @return A deep copy of this object
   */
  public BitcoinConfiguration deepCopy() {

    BitcoinConfiguration bitcoin = new BitcoinConfiguration();

    bitcoin.setBitcoinSymbol(getBitcoinSymbol());

    return bitcoin;
  }

  public String getExchangeClassName() {
    return exchangeClassName;
  }

  public void setExchangeClassName(String exchangeClassName) {
    this.exchangeClassName = exchangeClassName;
  }

  /**
   * @return The friendly exchange name (e.g. "Mt Gox", "Bitstamp" etc)
   */
  public String getExchangeName() {
    return exchangeName;
  }

  public void setExchangeName(String exchangeName) {
    this.exchangeName = exchangeName;
  }
}
