package qualm;

public class Patch {
  String id = null;
  String bank = null;
  int number = -1;
  String description;

  public Patch( String id, int number ) {
    this.id = id;
    this.number = number;
  } 

  /**
   * Get the ID value.
   * @return the ID value.
   */
  public String getID() {
    return id;
  }

  /**
   * Set the ID value.
   * @param newID The new ID value.
   */
  public void setID(String newID) {
    this.id = newID;
  }

  /**
   * Get the Bank value.
   * @return the Bank value.
   */
  public String getBank() {
    return bank;
  }

  /**
   * Set the Bank value.
   * @param newBank The new Bank value.
   */
  public void setBank(String newBank) {
    this.bank = newBank;
  }

  /**
   * Get the Number value.
   * @return the Number value.
   */
  public int getNumber() {
    return number;
  }

  /**
   * Set the Number value.
   * @param newNumber The new Number value.
   */
  public void setNumber(int newNumber) {
    this.number = newNumber;
  }

  /**
   * Get the Description value.
   * @return the Description value.
   */
  public String getDescription() {
    return description;
  }

  /**
   * Set the Description value.
   * @param newDescription The new Description value.
   */
  public void setDescription(String newDescription) {
    this.description = newDescription;
  }

  public String toString() { 
    return getID();
  }
  
} 
