/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package org.kosta.model;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.adempiere.base.Core;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.*;
import org.compiere.process.DocAction;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;


/**
 *  Order Model.
 * 	Please do not set DocStatus and C_DocType_ID directly. 
 * 	They are set in the process() method. 
 * 	Use DocAction and C_DocTypeTarget_ID instead.
 *
 *  @author Jorg Janke
 *
 *  @author victor.perez@e-evolution.com, e-Evolution http://www.e-evolution.com
 * 			<li> FR [ 2520591 ] Support multiples calendar for Org 
 *			@see http://sourceforge.net/tracker2/?func=detail&atid=879335&aid=2520591&group_id=176962
 *  @version $Id: MOrder.java,v 1.5 2006/10/06 00:42:24 jjanke Exp $
 * 
 * @author Teo Sarca, www.arhipac.ro
 * 			<li>BF [ 2419978 ] Voiding PO, requisition don't set on NULL
 * 			<li>BF [ 2892578 ] Order should autoset only active price lists
 * 				https://sourceforge.net/tracker/?func=detail&aid=2892578&group_id=176962&atid=879335
 * @author Michael Judd, www.akunagroup.com
 *          <li>BF [ 2804888 ] Incorrect reservation of products with attributes
 */
public class MUMOrder extends MOrder implements DocAction
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -5424713436299981736L;

	/**
	 * 	Create new Order by copying
	 * 	@param from order
	 * 	@param dateDoc date of the document date
	 * 	@param C_DocTypeTarget_ID target document type
	 * 	@param isSOTrx sales order 
	 * 	@param counter create counter links
	 *	@param copyASI copy line attributes Attribute Set Instance, Resaouce Assignment
	 * 	@param trxName trx
	 *	@return Order
	 */
	public static MUMOrder copyFrom (MUMOrder from, Timestamp dateDoc, 
		int C_DocTypeTarget_ID, boolean isSOTrx, boolean counter, boolean copyASI, 
		String trxName)
	{
		MUMOrder to = new MUMOrder (from.getCtx(), 0, trxName);
		to.set_TrxName(trxName);
		PO.copyValues(from, to, from.getAD_Client_ID(), from.getAD_Org_ID());
		to.set_ValueNoCheck ("C_Order_ID", I_ZERO);
		to.set_ValueNoCheck ("DocumentNo", null);
		//
		to.setDocStatus (DOCSTATUS_Drafted);		//	Draft
		to.setDocAction(DOCACTION_Complete);
		//
		to.setC_DocType_ID(0);
		to.setC_DocTypeTarget_ID (C_DocTypeTarget_ID);
		to.setIsSOTrx(isSOTrx);
		//
		to.setIsSelected (false);
		to.setDateOrdered (dateDoc);
		to.setDateAcct (dateDoc);
		to.setDatePromised (dateDoc);	//	assumption
		to.setDatePrinted(null);
		to.setIsPrinted (false);
		//
		to.setIsApproved (false);
		to.setIsCreditApproved(false);
		to.setC_Payment_ID(0);
		to.setC_CashLine_ID(0);
		//	Amounts are updated  when adding lines
		to.setGrandTotal(Env.ZERO);
		to.setTotalLines(Env.ZERO);
		//
		to.setIsDelivered(false);
		to.setIsInvoiced(false);
		to.setIsSelfService(false);
		to.setIsTransferred (false);
		to.setPosted (false);
		to.setProcessed (false);
		if (counter)
			to.setRef_Order_ID(from.getC_Order_ID());
		else
			to.setRef_Order_ID(0);
		//
		if (!to.save(trxName))
			throw new IllegalStateException("Could not create Order");
		if (counter)
			from.setRef_Order_ID(to.getC_Order_ID());

		if (to.copyLinesFrom(from, counter, copyASI) == 0)
			throw new IllegalStateException("Could not create Order Lines");
		
		// don't copy linked PO/SO
		to.setLink_Order_ID(0);
		
		return to;
	}	//	copyFrom
	
	
	/**************************************************************************
	 *  Default Constructor
	 *  @param ctx context
	 *  @param  C_Order_ID    order to load, (0 create new order)
	 *  @param trxName trx name
	 */
	public MUMOrder(Properties ctx, int C_Order_ID, String trxName)
	{
		super (ctx, C_Order_ID, trxName);
		//  New
		if (C_Order_ID == 0)
		{
			setDocStatus(DOCSTATUS_Drafted);
			setDocAction (DOCACTION_Prepare); 
			//
			setDeliveryRule (DELIVERYRULE_Availability);
			setFreightCostRule (FREIGHTCOSTRULE_FreightIncluded);
			setInvoiceRule (INVOICERULE_Immediate);
			setPaymentRule(PAYMENTRULE_OnCredit);
			setPriorityRule (PRIORITYRULE_Medium);
			setDeliveryViaRule (DELIVERYVIARULE_Pickup);
			//
			setIsDiscountPrinted (false);
			setIsSelected (false);
			setIsTaxIncluded (false);
			setIsSOTrx (true);
			setIsDropShip(false);
			setSendEMail (false);
			//
			setIsApproved(false);
			setIsPrinted(false);
			setIsCreditApproved(false);
			setIsDelivered(false);
			setIsInvoiced(false);
			setIsTransferred(false);
			setIsSelfService(false);
			//
			super.setProcessed(false);
			setProcessing(false);
			setPosted(false);

			setDateAcct (new Timestamp(System.currentTimeMillis()));
			setDatePromised (new Timestamp(System.currentTimeMillis()));
			setDateOrdered (new Timestamp(System.currentTimeMillis()));

			setFreightAmt (Env.ZERO);
			setChargeAmt (Env.ZERO);
			setTotalLines (Env.ZERO);
			setGrandTotal (Env.ZERO);
		}
	}	//	MOrder

	/**************************************************************************
	 *  Project Constructor
	 *  @param  project Project to create Order from
	 *  @param IsSOTrx sales order
	 * 	@param	DocSubTypeSO if SO DocType Target (default DocSubTypeSO_OnCredit)
	 */
	public MUMOrder (MProject project, boolean IsSOTrx, String DocSubTypeSO)
	{
		this (project.getCtx(), 0, project.get_TrxName());
		setAD_Client_ID(project.getAD_Client_ID());
		setAD_Org_ID(project.getAD_Org_ID());
		setC_Campaign_ID(project.getC_Campaign_ID());
		setSalesRep_ID(project.getSalesRep_ID());
		//
		setC_Project_ID(project.getC_Project_ID());
		setDescription(project.getName());
		Timestamp ts = project.getDateContract();
		if (ts != null)
			setDateOrdered (ts);
		ts = project.getDateFinish();
		if (ts != null)
			setDatePromised (ts);
		//
		setC_BPartner_ID(project.getC_BPartner_ID());
		setC_BPartner_Location_ID(project.getC_BPartner_Location_ID());
		setAD_User_ID(project.getAD_User_ID());
		//
		setM_Warehouse_ID(project.getM_Warehouse_ID());
		setM_PriceList_ID(project.getM_PriceList_ID());
		setC_PaymentTerm_ID(project.getC_PaymentTerm_ID());
		//
		setIsSOTrx(IsSOTrx);
		if (IsSOTrx)
		{
			if (DocSubTypeSO == null || DocSubTypeSO.length() == 0)
				setC_DocTypeTarget_ID(DocSubTypeSO_OnCredit);
			else
				setC_DocTypeTarget_ID(DocSubTypeSO);
		}
		else
			setC_DocTypeTarget_ID();
	}	//	MOrder

	/**
	 *  Load Constructor
	 *  @param ctx context
	 *  @param rs result set record
	 *  @param trxName transaction
	 */
	public MUMOrder (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MOrder
	
	/**	Process Message 			*/
	private String		m_processMsg = null;
	/**	Just Prepared Flag			*/
	private boolean		m_justPrepared = false;
	
	/** Force Creation of order		*/
	private boolean			m_forceCreation = false;

	private boolean reserveStock (MDocType dt, MOrderLine[] lines)
	{
		if (dt == null)
			dt = MDocType.get(getCtx(), getC_DocType_ID());

		//	Binding
		boolean binding = !dt.isProposal();
		//	Not binding - i.e. Target=0
		if (DOCACTION_Void.equals(getDocAction())
			//	Closing Binding Quotation
			|| (MDocType.DOCSUBTYPESO_Quotation.equals(dt.getDocSubTypeSO()) 
				&& DOCACTION_Close.equals(getDocAction())) 
			) // || isDropShip() )
			binding = false;
		boolean isSOTrx = isSOTrx();
		if (log.isLoggable(Level.FINE)) log.fine("Binding=" + binding + " - IsSOTrx=" + isSOTrx);
		//	Force same WH for all but SO/PO
		int header_M_Warehouse_ID = getM_Warehouse_ID();
		if (MDocType.DOCSUBTYPESO_StandardOrder.equals(dt.getDocSubTypeSO())
			|| MDocType.DOCBASETYPE_PurchaseOrder.equals(dt.getDocBaseType()))
			header_M_Warehouse_ID = 0;		//	don't enforce
		
		BigDecimal Volume = Env.ZERO;
		BigDecimal Weight = Env.ZERO;
		
		//	Always check and (un) Reserve Inventory		
		for (int i = 0; i < lines.length; i++)
		{
			MOrderLine line = lines[i];
			//	Check/set WH/Org
			if (header_M_Warehouse_ID != 0)	//	enforce WH
			{
				if (header_M_Warehouse_ID != line.getM_Warehouse_ID())
					line.setM_Warehouse_ID(header_M_Warehouse_ID);
				if (getAD_Org_ID() != line.getAD_Org_ID())
					line.setAD_Org_ID(getAD_Org_ID());
			}
			//	Binding
			BigDecimal target = binding ? line.getQtyOrdered() : Env.ZERO; 
			BigDecimal difference = target
				.subtract(line.getQtyReserved())
				.subtract(line.getQtyDelivered()); 
			if (difference.signum() == 0)
			{
				MProduct product = line.getProduct();
				if (product != null)
				{
					Volume = Volume.add(product.getVolume().multiply(line.getQtyOrdered()));
					Weight = Weight.add(product.getWeight().multiply(line.getQtyOrdered()));
				}
				continue;
			}
			
			if (log.isLoggable(Level.FINE)) log.fine("Line=" + line.getLine() 
				+ " - Target=" + target + ",Difference=" + difference
				+ " - Ordered=" + line.getQtyOrdered() 
				+ ",Reserved=" + line.getQtyReserved() + ",Delivered=" + line.getQtyDelivered());

			//	Check Product - Stocked and Item
			MProduct product = line.getProduct();
			if (product != null) 
			{
				if (product.isStocked())
				{
					//	Update Reservation Storage
					if (!MStorageReservation.add(getCtx(), line.getM_Warehouse_ID(), 
						line.getM_Product_ID(), 
						line.getM_AttributeSetInstance_ID(), line.getM_AttributeSetInstance_ID(),
						difference, isSOTrx, get_TrxName()))
						return false;
				}	//	stocked
				//	update line
				line.setQtyReserved(line.getQtyReserved().add(difference));
				if (!line.save(get_TrxName()))
					return false;
				//
				Volume = Volume.add(product.getVolume().multiply(line.getQtyOrdered()));
				Weight = Weight.add(product.getWeight().multiply(line.getQtyOrdered()));
			}	//	product
		}	//	reverse inventory
		
		setVolume(Volume);
		setWeight(Weight);
		return true;
	}	//	reserveStock
	
	private void setDefiniteDocumentNo() {
		MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());
		if (dt.isOverwriteDateOnComplete()) {
			/* a42niem - BF IDEMPIERE-63 - check if document has been completed before */ 
			if (this.getProcessedOn().signum() == 0) {
				setDateOrdered(new Timestamp (System.currentTimeMillis()));
			}
		}
		if (dt.isOverwriteSeqOnComplete()) {
			/* a42niem - BF IDEMPIERE-63 - check if document has been completed before */ 
			if (this.getProcessedOn().signum() == 0) {
				String value = DB.getDocumentNo(getC_DocType_ID(), get_TrxName(), true, this);
				if (value != null)
					setDocumentNo(value);
			}
		}
	} // setDefiniteDocumentNo
	
	private MInOut createShipment(MDocType dt, Timestamp movementDate)
	{
		if (log.isLoggable(Level.INFO)) log.info("For " + dt);
		MInOut shipment = new MInOut (this, dt.getC_DocTypeShipment_ID(), movementDate);
	//	shipment.setDateAcct(getDateAcct());
		if (!shipment.save(get_TrxName()))
		{
			m_processMsg = "Could not create Shipment";
			return null;
		}
		//
		MOrderLine[] oLines = getLines(true, null);
		for (int i = 0; i < oLines.length; i++)
		{
			MOrderLine oLine = oLines[i];
			//
			MInOutLine ioLine = new MInOutLine(shipment);
			//	Qty = Ordered - Delivered
			BigDecimal MovementQty = oLine.getQtyOrdered().subtract(oLine.getQtyDelivered()); 
			//	Location
			int M_Locator_ID = MStorageOnHand.getM_Locator_ID (oLine.getM_Warehouse_ID(), 
					oLine.getM_Product_ID(), oLine.getM_AttributeSetInstance_ID(), 
					MovementQty, get_TrxName());
			if (M_Locator_ID == 0)		//	Get default Location
			{
				MWarehouse wh = MWarehouse.get(getCtx(), oLine.getM_Warehouse_ID());
				M_Locator_ID = wh.getDefaultLocator().getM_Locator_ID();
			}
			//
			ioLine.setOrderLine(oLine, M_Locator_ID, MovementQty);
			ioLine.setQty(MovementQty);
			if (oLine.getQtyEntered().compareTo(oLine.getQtyOrdered()) != 0)
				ioLine.setQtyEntered(MovementQty
					.multiply(oLine.getQtyEntered())
					.divide(oLine.getQtyOrdered(), 6, BigDecimal.ROUND_HALF_UP));
			if (!ioLine.save(get_TrxName()))
			{
				m_processMsg = "Could not create Shipment Line";
				return null;
			}
		}
		// added AdempiereException by zuhri
		if (!shipment.processIt(DocAction.ACTION_Complete))
			throw new AdempiereException("Failed when processing document - " + shipment.getProcessMsg());
		// end added
		shipment.saveEx(get_TrxName());
		if (!DOCSTATUS_Completed.equals(shipment.getDocStatus()))
		{
			m_processMsg = "@M_InOut_ID@: " + shipment.getProcessMsg();
			return null;
		}
		return shipment;
	}	//	createShipment

	
	public String completeIt()
	{
		MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());
		String DocSubTypeSO = dt.getDocSubTypeSO();
		
		//	Just prepare
		if (DOCACTION_Prepare.equals(getDocAction()))
		{
			setProcessed(false);
			return DocAction.STATUS_InProgress;
		}
		//	Offers
		if (MDocType.DOCSUBTYPESO_Proposal.equals(DocSubTypeSO)
			|| MDocType.DOCSUBTYPESO_Quotation.equals(DocSubTypeSO)) 
		{
			//	Binding
			if (MDocType.DOCSUBTYPESO_Quotation.equals(DocSubTypeSO))
				reserveStock(dt, getLines(true, MOrderLine.COLUMNNAME_M_Product_ID));
			m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_COMPLETE);
			if (m_processMsg != null)
				return DocAction.STATUS_Invalid;
			m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_COMPLETE);
			if (m_processMsg != null)
				return DocAction.STATUS_Invalid;
			// Set the definite document number after completed (if needed)
			setDefiniteDocumentNo();
			setProcessed(true);
			return DocAction.STATUS_Completed;
		}
		//	Waiting Payment - until we have a payment
		if (!m_forceCreation 
			&& MDocType.DOCSUBTYPESO_PrepayOrder.equals(DocSubTypeSO) 
			&& getC_Payment_ID() == 0 && getC_CashLine_ID() == 0)
		{
			setProcessed(true);
			return DocAction.STATUS_WaitingPayment;
		}
		
		//	Re-Check
		if (!m_justPrepared)
		{
			String status = prepareIt();
			if (!DocAction.STATUS_InProgress.equals(status))
				return status;
		}
		
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_COMPLETE);
		if (m_processMsg != null)
			return DocAction.STATUS_Invalid;
		
		//	Implicit Approval
		if (!isApproved())
			approveIt();
		getLines(true,null);
		if (log.isLoggable(Level.INFO)) log.info(toString());
		StringBuilder info = new StringBuilder();
		
		boolean realTimePOS = MSysConfig.getBooleanValue(MSysConfig.REAL_TIME_POS, false , getAD_Client_ID());
		
		//	Create SO Shipment - Force Shipment
		MInOut shipment = null;
		if (MDocType.DOCSUBTYPESO_OnCreditOrder.equals(DocSubTypeSO)		//	(W)illCall(I)nvoice
			|| MDocType.DOCSUBTYPESO_WarehouseOrder.equals(DocSubTypeSO)	//	(W)illCall(P)ickup	
			|| MDocType.DOCSUBTYPESO_POSOrder.equals(DocSubTypeSO)			//	(W)alkIn(R)eceipt
			|| MDocType.DOCSUBTYPESO_PrepayOrder.equals(DocSubTypeSO)) 
		{
			if (!DELIVERYRULE_Force.equals(getDeliveryRule()))
			{
				MWarehouse wh = new MWarehouse (getCtx(), getM_Warehouse_ID(), get_TrxName());
				if (!wh.isDisallowNegativeInv())
					setDeliveryRule(DELIVERYRULE_Force);
			}
			//
			shipment = createShipment (dt, realTimePOS ? null : getDateOrdered());
			if (shipment == null)
				return DocAction.STATUS_Invalid;
			info.append("@M_InOut_ID@: ").append(shipment.getDocumentNo());
			String msg = shipment.getProcessMsg();
			if (msg != null && msg.length() > 0)
				info.append(" (").append(msg).append(")");
		}	//	Shipment
		

		//	Create SO Invoice - Always invoice complete Order
		if ( MDocType.DOCSUBTYPESO_POSOrder.equals(DocSubTypeSO)
			|| MDocType.DOCSUBTYPESO_OnCreditOrder.equals(DocSubTypeSO) 	
			|| MDocType.DOCSUBTYPESO_PrepayOrder.equals(DocSubTypeSO)) 
		{			
			MInvoice invoice = createInvoice (dt, shipment, realTimePOS ? null : getDateOrdered());
			if (invoice == null)
				return DocAction.STATUS_Invalid;
			info.append(" - @C_Invoice_ID@: ").append(invoice.getDocumentNo());
			String msg = invoice.getProcessMsg();
			if (msg != null && msg.length() > 0)
				info.append(" (").append(msg).append(")");
			
		}	//	Invoice
		
		String msg = createPOSPayments();
		if (msg != null) {
			m_processMsg = msg;
			return DocAction.STATUS_Invalid;
		}

		//	Counter Documents
		MOrder counter = createCounterDoc();
		if (counter != null)
			info.append(" - @CounterDoc@: @Order@=").append(counter.getDocumentNo());
		//	User Validation
		String valid = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_COMPLETE);
		if (valid != null)
		{
			if (info.length() > 0)
				info.append(" - ");
			info.append(valid);
			m_processMsg = info.toString();
			return DocAction.STATUS_Invalid;
		}

		//landed cost
		if (!isSOTrx())
		{
			String error = landedCostAllocation();
			if (!Util.isEmpty(error))
			{
				m_processMsg = error;
				return DocAction.STATUS_Invalid;
			}
		}
		
		// Set the definite document number after completed (if needed)
		setDefiniteDocumentNo();

		setProcessed(true);	
		m_processMsg = info.toString();
		//
		setDocAction(DOCACTION_Close);
		return DocAction.STATUS_Completed;
	}	//	completeIt
	
	private String createPOSPayments() {

		// Just for POS order with payment rule mixed
		if (! this.isSOTrx())
			return null;
		if (! MOrder.DocSubTypeSO_POS.equals(this.getC_DocType().getDocSubTypeSO()))
			return null;
		if (! MOrder.PAYMENTRULE_MixedPOSPayment.equals(this.getPaymentRule()))
			return null;

		// Verify sum of all payments pos must be equal to the grandtotal of POS invoice (minus withholdings)
		MInvoice[] invoices = getInvoice();
		if (invoices == null || invoices.length == 0)
			return "@NoPOSInvoices@";
		MInvoice lastInvoice = invoices[0];
		BigDecimal grandTotal = lastInvoice.getGrandTotal();
		
		List<X_C_POSPayment> pps = new Query(this.getCtx(), X_C_POSPayment.Table_Name, "C_Order_ID=?", this.get_TrxName())
			.setParameters(this.getC_Order_ID())
			.setOnlyActiveRecords(true)
			.list();
		BigDecimal totalPOSPayments = Env.ZERO; 
		for (X_C_POSPayment pp : pps) {
			totalPOSPayments = totalPOSPayments.add(pp.getPayAmt());
		}
		if (totalPOSPayments.compareTo(grandTotal) != 0)
			return "@POSPaymentDiffers@ - @C_POSPayment_ID@=" + totalPOSPayments + ", @GrandTotal@=" + grandTotal;
		
		String whereClause = "AD_Org_ID=? AND C_Currency_ID=?";
		MBankAccount ba = new Query(this.getCtx(),MBankAccount.Table_Name,whereClause,this.get_TrxName())
			.setParameters(this.getAD_Org_ID(), this.getC_Currency_ID())
			.setOrderBy("IsDefault DESC")
			.first();
		if (ba == null)
			return "@NoAccountOrgCurrency@";
		
		
		MDocType[] doctypes = MDocType.getOfDocBaseType(this.getCtx(), MDocType.DOCBASETYPE_ARReceipt);
		if (doctypes == null || doctypes.length == 0)
			return "No document type for AR Receipt";
		MDocType doctype = null;
		for (MDocType doc : doctypes) {
			if (doc.getAD_Org_ID() == this.getAD_Org_ID()) {
				doctype = doc;
				break;
			}
		}
		if (doctype == null)
			doctype = doctypes[0];

		// Create a payment for each non-guarantee record
		// associate the payment id and mark the record as processed
		for (X_C_POSPayment pp : pps) {
			X_C_POSTenderType  tt = new X_C_POSTenderType (getCtx(),pp.getC_POSTenderType_ID(), get_TrxName());
			if (tt.isGuarantee())
				continue;
			if (pp.isPostDated())
				continue;

			MPayment payment = new MPayment(this.getCtx(), 0, this.get_TrxName());
			payment.setAD_Org_ID(this.getAD_Org_ID());

			payment.setTenderType(pp.getTenderType());
			if (MPayment.TENDERTYPE_CreditCard.equals(pp.getTenderType())) {
				payment.setTrxType(MPayment.TRXTYPE_Sales);
				payment.setCreditCardType(pp.getCreditCardType());
				payment.setCreditCardNumber(pp.getCreditCardNumber());
				payment.setVoiceAuthCode(pp.getVoiceAuthCode());
			}

			payment.setC_BankAccount_ID(ba.getC_BankAccount_ID());
			payment.setRoutingNo(pp.getRoutingNo());
			payment.setAccountNo(pp.getAccountNo());
			payment.setCheckNo(pp.getCheckNo());
			payment.setMicr(pp.getMicr());
			payment.setIsPrepayment(false);
			
			payment.setDateAcct(this.getDateAcct());
			payment.setDateTrx(this.getDateOrdered());
			//
			payment.setC_BPartner_ID(this.getC_BPartner_ID());
			payment.setC_Invoice_ID(lastInvoice.getC_Invoice_ID());
			// payment.setC_Order_ID(this.getC_Order_ID()); / do not set order to avoid the prepayment flag
			payment.setC_DocType_ID(doctype.getC_DocType_ID());
			payment.setC_Currency_ID(this.getC_Currency_ID());

			payment.setPayAmt(pp.getPayAmt());

			//	Copy statement line reference data
			payment.setA_Name(pp.getA_Name());
			
			payment.setC_POSTenderType_ID(pp.getC_POSTenderType_ID());
			
			//	Save payment
			payment.saveEx();

			pp.setC_Payment_ID(payment.getC_Payment_ID());
			pp.setProcessed(true);
			pp.saveEx();

			payment.setDocAction(MPayment.DOCACTION_Complete);
			if (!payment.processIt (MPayment.DOCACTION_Complete))
				return "Cannot Complete the Payment :" + payment;

			payment.saveEx();
		}

		return null;
	} // createPosPayment
	
	private MInvoice createInvoice (MDocType dt, MInOut shipment, Timestamp invoiceDate)
	{
		if (log.isLoggable(Level.INFO)) log.info(dt.toString());
		MInvoice invoice = new MInvoice (this, dt.getC_DocTypeInvoice_ID(), invoiceDate);
		if (!invoice.save(get_TrxName()))
		{
			m_processMsg = "Could not create Invoice";
			return null;
		}
		
		//	If we have a Shipment - use that as a base
		if (shipment != null)
		{
			if (!INVOICERULE_AfterDelivery.equals(getInvoiceRule()))
				setInvoiceRule(INVOICERULE_AfterDelivery);
			//
			MInOutLine[] sLines = shipment.getLines(false);
			for (int i = 0; i < sLines.length; i++)
			{
				MInOutLine sLine = sLines[i];
				// add by ah
				// type casting
				MInvoice mInvoice = new MInvoice(getCtx(), invoice.getC_Invoice_ID(), get_TrxName());
				MInvoiceLine iLine = new MInvoiceLine(mInvoice);
				iLine.setShipLine(sLine);
				//	Qty = Delivered	
				if (sLine.sameOrderLineUOM())
					iLine.setQtyEntered(sLine.getQtyEntered());
				else
					iLine.setQtyEntered(sLine.getMovementQty());
				iLine.setQtyInvoiced(sLine.getMovementQty());
				if (!iLine.save(get_TrxName()))
				{
					m_processMsg = "Could not create Invoice Line from Shipment Line";
					return null;
				}
				//
				sLine.setIsInvoiced(true);
				if (!sLine.save(get_TrxName()))
				{
					log.warning("Could not update Shipment line: " + sLine);
				}
			}
		}
		else	//	Create Invoice from Order
		{
			if (!INVOICERULE_Immediate.equals(getInvoiceRule()))
				setInvoiceRule(INVOICERULE_Immediate);
			//
			MOrderLine[] oLines = getLines();
			for (int i = 0; i < oLines.length; i++)
			{
				MOrderLine oLine = oLines[i];
				// add by ah
				// type casting
				MInvoice mInvoice = new MInvoice(getCtx(), invoice.getC_Invoice_ID(), get_TrxName());
				MInvoiceLine iLine = new MInvoiceLine(mInvoice);
				iLine.setOrderLine(oLine);
				//	Qty = Ordered - Invoiced	
				iLine.setQtyInvoiced(oLine.getQtyOrdered().subtract(oLine.getQtyInvoiced()));
				if (oLine.getQtyOrdered().compareTo(oLine.getQtyEntered()) == 0)
					iLine.setQtyEntered(iLine.getQtyInvoiced());
				else
					iLine.setQtyEntered(iLine.getQtyInvoiced().multiply(oLine.getQtyEntered())
						.divide(oLine.getQtyOrdered(), 12, BigDecimal.ROUND_HALF_UP));
				if (!iLine.save(get_TrxName()))
				{
					m_processMsg = "Could not create Invoice Line from Order Line";
					return null;
				}
			}
		}
		
		// Copy payment schedule from order to invoice if any
		for (MOrderPaySchedule ops : MOrderPaySchedule.getOrderPaySchedule(getCtx(), getC_Order_ID(), 0, get_TrxName())) {
			MInvoicePaySchedule ips = new MInvoicePaySchedule(getCtx(), 0, get_TrxName());
			PO.copyValues(ops, ips);
			ips.setC_Invoice_ID(invoice.getC_Invoice_ID());
			ips.setAD_Org_ID(ops.getAD_Org_ID());
			ips.setProcessing(ops.isProcessing());
			ips.setIsActive(ops.isActive());
			if (!ips.save()) {
				m_processMsg = "ERROR: creating pay schedule for invoice from : "+ ops.toString();
				return null;
			}
		}
		
		// added AdempiereException by zuhri
		if (!invoice.processIt(DocAction.ACTION_Complete))
			throw new AdempiereException("Failed when processing document - " + invoice.getProcessMsg());
		// end added
		invoice.saveEx(get_TrxName());
		setC_CashLine_ID(invoice.getC_CashLine_ID());
		if (!DOCSTATUS_Completed.equals(invoice.getDocStatus()))
		{
			m_processMsg = "@C_Invoice_ID@: " + invoice.getProcessMsg();
			return null;
		}
		return invoice;
	}	//	createInvoices
	
	private MOrder createCounterDoc()
	{
		//	Is this itself a counter doc ?
		if (getRef_Order_ID() != 0)
			return null;
		
		//	Org Must be linked to BPartner
		MOrg org = MOrg.get(getCtx(), getAD_Org_ID());
		int counterC_BPartner_ID = org.getLinkedC_BPartner_ID(get_TrxName()); 
		if (counterC_BPartner_ID == 0)
			return null;
		//	Business Partner needs to be linked to Org
		MBPartner bp = new MBPartner (getCtx(), getC_BPartner_ID(), get_TrxName());
		int counterAD_Org_ID = bp.getAD_OrgBP_ID_Int(); 
		if (counterAD_Org_ID == 0)
			return null;
		
		MBPartner counterBP = new MBPartner (getCtx(), counterC_BPartner_ID, null);
		MOrgInfo counterOrgInfo = MOrgInfo.get(getCtx(), counterAD_Org_ID, get_TrxName());
		if (log.isLoggable(Level.INFO)) log.info("Counter BP=" + counterBP.getName());

		//	Document Type
		int C_DocTypeTarget_ID = 0;
		MDocTypeCounter counterDT = MDocTypeCounter.getCounterDocType(getCtx(), getC_DocType_ID());
		if (counterDT != null)
		{
			if (log.isLoggable(Level.FINE)) log.fine(counterDT.toString());
			if (!counterDT.isCreateCounter() || !counterDT.isValid())
				return null;
			C_DocTypeTarget_ID = counterDT.getCounter_C_DocType_ID();
		}
		else	//	indirect
		{
			C_DocTypeTarget_ID = MDocTypeCounter.getCounterDocType_ID(getCtx(), getC_DocType_ID());
			if (log.isLoggable(Level.FINE)) log.fine("Indirect C_DocTypeTarget_ID=" + C_DocTypeTarget_ID);
			if (C_DocTypeTarget_ID <= 0)
				return null;
		}
		//	Deep Copy
		MOrder counter = copyFrom (this, getDateOrdered(), 
			C_DocTypeTarget_ID, !isSOTrx(), true, false, get_TrxName());
		//
		counter.setAD_Org_ID(counterAD_Org_ID);
		counter.setM_Warehouse_ID(counterOrgInfo.getM_Warehouse_ID());
		//
		counter.setBPartner(counterBP);
		counter.setDatePromised(getDatePromised());		// default is date ordered 
		//	Refernces (Should not be required
		counter.setSalesRep_ID(getSalesRep_ID());
		counter.saveEx(get_TrxName());
		
		//	Update copied lines
		MOrderLine[] counterLines = counter.getLines(true, null);
		for (int i = 0; i < counterLines.length; i++)
		{
			MOrderLine counterLine = counterLines[i];
			counterLine.setOrder(counter);	//	copies header values (BP, etc.)
			counterLine.setPrice();
			counterLine.setTax();
			counterLine.saveEx(get_TrxName());
		}
		if (log.isLoggable(Level.FINE)) log.fine(counter.toString());
		
		//	Document Action
		if (counterDT != null)
		{
			if (counterDT.getDocAction() != null)
			{
				counter.setDocAction(counterDT.getDocAction());
				// added AdempiereException by zuhri
				if (!counter.processIt(counterDT.getDocAction()))
					throw new AdempiereException("Failed when processing document - " + counter.getProcessMsg());
				// end added
				counter.saveEx(get_TrxName());
			}
		}
		return counter;
	}	//	createCounterDoc
	
	private String landedCostAllocation() {
		MOrderLandedCost[] landedCosts = MOrderLandedCost.getOfOrder(getC_Order_ID(), get_TrxName());
		for(MOrderLandedCost landedCost : landedCosts) {
			String error = landedCost.distributeLandedCost();
			if (!Util.isEmpty(error))
				return error;
		}
		return "";
	}
	
	
	public MInvoice[] getInvoice()
	{
		final String whereClause = "EXISTS (SELECT 1 FROM C_InvoiceLine il, C_OrderLine ol"
							        +" WHERE il.C_Invoice_ID=C_Invoice.C_Invoice_ID"
							        		+" AND il.C_OrderLine_ID=ol.C_OrderLine_ID"
							        		+" AND ol.C_Order_ID=?)";
		List<MInvoice> list = new Query(getCtx(), I_C_Invoice.Table_Name, whereClause, get_TrxName())
									.setParameters(get_ID())
									.setOrderBy("C_Invoice_ID DESC")
									.list();
		return list.toArray(new MInvoice[list.size()]);
	}	//	getInvoices
	
		
}	//	MOrder
