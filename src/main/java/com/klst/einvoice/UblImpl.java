package com.klst.einvoice;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import org.compiere.model.MBPBankAccount;
import org.compiere.model.MBank;
import org.compiere.model.MBankAccount;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MLocation;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MPaymentTerm;
import org.compiere.model.MUser;
import org.compiere.util.DB;
import org.compiere.util.Env;

import com.klst.einvoice.ubl.Address;
import com.klst.einvoice.ubl.Contact;
import com.klst.einvoice.ubl.FinancialAccount;
import com.klst.einvoice.ubl.PaymentMandate;
import com.klst.einvoice.ubl.VatBreakdown;
import com.klst.einvoice.unece.uncefact.Amount;
import com.klst.einvoice.unece.uncefact.BICId;
import com.klst.einvoice.unece.uncefact.IBANId;
import com.klst.marshaller.UblCreditNoteTransformer;
import com.klst.marshaller.UblInvoiceTransformer;
import com.klst.untdid.codelist.PaymentMeansEnum;
import com.klst.untdid.codelist.TaxCategoryCode;

public class UblImpl extends AbstractEinvoice {

	private static final Logger LOG = Logger.getLogger(UblImpl.class.getName());

	private UblImpl delegate;
	
	// ctor
	public UblImpl() {
		super();
		delegate = null;
	}

	@Override
	public String getDocumentNo() {
		return delegate.getDocumentNo();
	}

	protected Address mapLocationToAddress(int location_ID) {
		MLocation mLocation = new MLocation(Env.getCtx(), location_ID, get_TrxName());
		String countryCode = mLocation.getCountry().getCountryCode();
		String postalCode = mLocation.getPostal();
		String city = mLocation.getCity();
		String street = null;
		String a1 = mLocation.getAddress1();
		String a2 = mLocation.getAddress2();
		String a3 = mLocation.getAddress3();
		String a4 = mLocation.getAddress4();
		Address address = new Address(countryCode, postalCode, city, street);
		if(a1!=null) address.setAddressLine1(a1);
		if(a2!=null) address.setAddressLine2(a2);
		if(a3!=null) address.setAddressLine3(a3);
		if(a4!=null) address.setAdditionalStreet(a4);
		return address;
	}
	
	protected Contact mapUserToContact(int user_ID) {
		MUser mUser = new MUser(Env.getCtx(), user_ID, get_TrxName());
		String contactName = mUser.getName();
		String contactTel = mUser.getPhone();
		String contactMail = mUser.getEMail();
		Contact contact = new Contact(contactName, contactTel, contactMail);
		return contact;
	}

	// TODO: idee mInvoice.get_xmlDocument ... für xRechnung nutzen!!!!
	// ist in PO als public org.w3c.dom.Document get_xmlDocument(boolean noComment)
	// nein ==> get_xmlDocument wird in PO.get_xmlString genutzt und das in HouseKeeping Process

	@Override
	public void setupTransformer(boolean isCreditNote) {
		if(isCreditNote) {
			transformer = UblCreditNoteTransformer.getInstance();
		} else {
			transformer = UblInvoiceTransformer.getInstance();
		}
	}

	@Override
	public byte[] tranformToXML(MInvoice mInvoice) {
		boolean isCreditNote = mInvoice.isCreditMemo();
		setupTransformer(isCreditNote);
		if(isCreditNote) {
			delegate = new UblCreditNote();
//			return transformer.fromModel(mapToUblCreditNote(mInvoice));			
		} else {
			delegate = new UblInvoice();
//			return transformer.fromModel(mapToEModel(mInvoice));			
		}
		return transformer.fromModel(delegate.mapToEModel(mInvoice));			
	}

	@Override
	Object mapToEModel(MInvoice mInvoice) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	void setBuyerReference(String buyerReference) {
		// TODO Auto-generated method stub
		
	}
	
	void setPaymentInstructions(PaymentMeansEnum code, String paymentMeansText, String remittanceInformation
			, CreditTransfer creditTransfer, PaymentCard paymentCard, DirectDebit directDebit) {
		// TODO
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.klst.einvoice.AbstractEinvoice#createCreditTransfer(com.klst.einvoice.unece.uncefact.IBANId, java.lang.String, com.klst.einvoice.unece.uncefact.BICId)
	 */
	// SEPA Überweisung	
	CreditTransfer createCreditTransfer(IBANId iban, String accountName, BICId bic) {
		return new FinancialAccount(iban, accountName, bic); // TODO static factory method
	}
	// non SEPA
	CreditTransfer createCreditTransfer(String accountId, String accountName, BICId bic) {
		return new FinancialAccount(accountId, accountName, bic); 
	}
	
	@Override
	DirectDebit createDirectDebit(String mandateID, String bankAssignedCreditorID, IBANId iban) {
		return PaymentMandate.createDirectDebit(mandateID, bankAssignedCreditorID, iban);
	}

	@Override
	DirectDebit createDirectDebit(String mandateID, String bankAssignedCreditorID, String debitedAccountID) {
		return PaymentMandate.createDirectDebit(mandateID, bankAssignedCreditorID, debitedAccountID);
	}


	void setPaymentTermsAndDate(String description, Timestamp ts) {
		
	}
	
//	String iban;
//	void getIBAN(String res, String iban) {
//		res = iban;
//	}
	String getDirectDebitIBAN(int partnerId) {
//		final String sql = "SELECT *"
//				+" FROM "+MBPBankAccount.Table_Name
//				+" WHERE "+MBPBankAccount.COLUMNNAME_C_BPartner_ID+"=?"
//				+" AND "+MBPBankAccount.COLUMNNAME_IsActive+"=?"; 
//		MBPBankAccount mBPBankAccount = new ...
		
//		String iban = null;
		List<MBPBankAccount> mBPBankAccountList = MBPBankAccount.getByPartner(Env.getCtx(), partnerId);
		mBPBankAccountList.forEach(mBPBankAccount -> {
			LOG.info("DirectDebit:"+mBPBankAccount.isDirectDebit() + " IBAN="+mBPBankAccount.getIBAN() + " "+mBPBankAccount);
//			getIBAN(iban, mBPBankAccount.getIBAN());
		});
		if(mBPBankAccountList.isEmpty()) return null;
		
		return mBPBankAccountList.get(0).getIBAN();
	}
	
	void makePaymentGroup() { // TODO nach oben und mit cii einheitlich
		int mAD_Org_ID = mInvoice.getAD_Org_ID(); // get AccountId of the Seller, aka AD_Org_ID for CreditTransfer
		MOrgInfo mOrgInfo = MOrgInfo.get(Env.getCtx(), mAD_Org_ID, get_TrxName());	
		MBank mBank = new MBank(Env.getCtx(), mOrgInfo.getTransferBank_ID(), get_TrxName());
		final String sql = "SELECT MIN("+MBankAccount.COLUMNNAME_C_BankAccount_ID+")"
				+" FROM "+MBankAccount.Table_Name
				+" WHERE "+MBankAccount.COLUMNNAME_C_Bank_ID+"=?"
				+" AND "+MBankAccount.COLUMNNAME_IsActive+"=?"; 
		int bankAccount_ID = DB.getSQLValueEx(get_TrxName(), sql, mOrgInfo.getTransferBank_ID(), true);
		MBankAccount mBankAccount = new MBankAccount(Env.getCtx(), bankAccount_ID, get_TrxName());
		
		String ddIBAN = getDirectDebitIBAN(mInvoice.getC_BPartner_ID()); // IBAN of the customer		
		
		String remittanceInformation = "TODO Verwendungszweck";
		if(mInvoice.getPaymentRule().equals(MInvoice.PAYMENTRULE_OnCredit) 
		|| mInvoice.getPaymentRule().equals(MInvoice.PAYMENTRULE_DirectDeposit)) {
			IBANId iban = new IBANId(mBankAccount.getIBAN());
			String paymentMeansText = null; // paymentMeansText : Text zur Zahlungsart
			CreditTransfer sepaCreditTransfer = createCreditTransfer(iban, null, null);
			setPaymentInstructions(PaymentMeansEnum.CreditTransfer, null, remittanceInformation, sepaCreditTransfer, null, null);
		} else if(mInvoice.getPaymentRule().equals(MInvoice.PAYMENTRULE_DirectDebit)) {
			IBANId iban = new IBANId(ddIBAN);
			String mandateID = null; // paymentMeansText : Text zur Zahlungsart
			DirectDebit sepaDirectDebit = createDirectDebit(mandateID, null, iban);
			setPaymentInstructions(PaymentMeansEnum.SEPADirectDebit, null, remittanceInformation, null, null, sepaDirectDebit);
		} else {
			LOG.warning("TODO PaymentMeansCode: mInvoice.PaymentRule="+mInvoice.getPaymentRule());
		}

		MPaymentTerm mPaymentTerm = new MPaymentTerm(Env.getCtx(), mInvoice.getC_PaymentTerm_ID(), get_TrxName());
//		((Invoice)ublObject).addPaymentTerms("#SKONTO#TAGE=7#PROZENT=2.00#"); // TODO
		setPaymentTermsAndDate(mPaymentTerm.getName(), (Timestamp)null); 
		LOG.info("finished.");
	}


	@Override
	void setTotals(Amount lineExtension, Amount taxExclusive, Amount taxInclusive, Amount payable, Amount taxTotal) {
		// TODO Auto-generated method stub
		
	}

	@Override
	void mapByuer(String buyerName, int location_ID, int user_ID) {
		// TODO Auto-generated method stub
		
	}

	@Override
	void mapSeller(String sellerName, int location_ID, int salesRep_ID, String companyID, String companyLegalForm,
			String taxCompanyId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	CoreInvoiceVatBreakdown createVatBreakdown(Amount taxableAmount, Amount taxAmount, TaxCategoryCode codeEnum, BigDecimal percent) {
		return new VatBreakdown(taxableAmount, taxAmount, codeEnum, percent);
	}

	@Override
	void addVATBreakDown(CoreInvoiceVatBreakdown vatBreakdown) {
		// TODO Auto-generated method stub in Subklasse
		
	}

	@Override
	void mapLine(MInvoiceLine line) {
		// TODO Auto-generated method stub
		
	}

}
