package org.elef.model;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.json.JSONArray;
import org.json.JSONObject;

public class MELF_ApiTemplate extends X_ELF_ApiTemplate{
	

	public MELF_ApiTemplate(Properties ctx, int ELF_ApiTemplate_ID, String trxName) {
		super(ctx, ELF_ApiTemplate_ID, trxName);
		// TODO Auto-generated constructor stub
	}


	public MELF_ApiTemplate(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
		// TODO Auto-generated constructor stub
	}

	private static final long serialVersionUID = -6658152177517159587L;
	
	// fetch elf_apitemplate_id from database
	public static int GetID(String trxName, Properties ctx, String elf_apiTemplate, int elf_apiconn_id) {
		// TODO Auto-generated method stub
		int elf_apitemplate_id = DB.getSQLValue(trxName, "select elf_apitemplate_id from elf_apitemplate where isactive = 'Y' and ad_client_id = ? and value = ? and elf_apiconn_id = ?", Env.getAD_Client_ID(ctx), elf_apiTemplate, elf_apiconn_id);
		
		return elf_apitemplate_id;
	}
	
	public static MELF_ApiTemplate getTemplate(String trxName, Properties ctx, String elf_apiTemplate,
			int elf_apiconn_id) {
		// TODO Auto-generated method stub
		int elf_apitemplate_id = GetID(trxName, ctx, elf_apiTemplate, elf_apiconn_id);
		MELF_ApiTemplate ElfApiTemplate = new MELF_ApiTemplate(ctx, elf_apitemplate_id, trxName);
		return ElfApiTemplate;
	}
	
	public String getRequestBody(PO po) {
		
		switch(getElf_ApiContentType()) {
		case "application/json":
			return getJsonRequestBody(po);
		}
            return null;
	}
	
	private String getJsonRequestBody(PO po) {
		// TODO Auto-generated method stub
		JSONObject requestJSONBody = new JSONObject();
		List<MELF_ApiField> apiFields = getApiFields("Y", "N", "Y");
		for (MELF_ApiField apiField : apiFields) {
			if(apiField.isParent()) {
				List<MELF_ApiField> childApiFields = getChildApiFields(apiField.get_ID());
				JSONObject childJSONBody = new JSONObject();
				for (MELF_ApiField childApiField : childApiFields) {
					Object value = po.get_Value(childApiField.getName());
					if (value != null  && value.toString().startsWith("http")) {
						String l_link = "<a href='" + value + "' target=\"_blank\">" + "link" + "</a>";
						childJSONBody.put(childApiField.getElf_ExtName(),l_link);
					}
					else 
						childJSONBody.put(childApiField.getElf_ExtName(), value);
				}
				requestJSONBody.put(apiField.getElf_ExtName(), childJSONBody);
			}
			else		
				if(apiField.isElf_setDefault())
                    requestJSONBody.put(apiField.getElf_ExtName(), apiField.getElf_DefaultValue() != null ?  apiField.getElf_DefaultValue():JSONObject.NULL );
                else 
                	if(po.columnExists(apiField.getName()))
                		requestJSONBody.put(apiField.getElf_ExtName(), po.get_Value(apiField.getName()));
		
		}
		return requestJSONBody.toString();
	}
	
	public void getJsonRequestBody(JSONObject requestJSONBody, List<?> list, String parentName) {
        // TODO Auto-generated method stub
	    if (list == null || list.isEmpty()) {
	        return;
	    }
	    
		//JSONObject childJSONBody = new JSONObject();
		MELF_ApiField apiParentField = null;
		List<MELF_ApiField> apiFields;
		if(parentName != null)
			apiParentField = getApiField(parentName);
		if(apiParentField != null) {
			apiFields = getChildApiFields(apiParentField.get_ID());
		}
		else
			apiFields = getApiFields("N", "N", "Y");
		
		if (list.get(0) instanceof List) {
			JSONArray childJSONArray = new JSONArray();
	        for (Object obj : list) {
	            List<?> list2 = (List<?>) obj;
	            JSONObject childJSONBody = new JSONObject();
	            for (MELF_ApiField apiField : apiFields) {
	                if (apiField.isElf_setDefault())
	                    childJSONBody.put(apiField.getElf_ExtName(), apiField.getElf_DefaultValue() != null ? apiField.getElf_DefaultValue() : JSONObject.NULL);
	                else
	                    childJSONBody.put(apiField.getElf_ExtName(), list2.get(apiField.getElf_listPosition()).toString());
	            }
	            childJSONArray.put(childJSONBody);
	        }
	        requestJSONBody.put(parentName, childJSONArray);
		}
		else {
			JSONObject childJSONBody = new JSONObject();
	        for (MELF_ApiField apiField : apiFields) {
	        	
	        	if(apiField.isElf_setDefault())
	        		childJSONBody.put(apiField.getElf_ExtName(), apiField.getElf_DefaultValue() != null ?  apiField.getElf_DefaultValue():JSONObject.NULL );
	        	else
	        		childJSONBody.put(apiField.getElf_ExtName(), list.get(apiField.getElf_listPosition()).toString());
	        }
	        requestJSONBody.put(parentName, childJSONBody);
		}
    }
	
	public void setJsonResponse(JSONObject responseJSONBody, PO po) {
        // TODO Auto-generated method stub
        List<MELF_ApiField> apiFields = getApiFields("N", "Y", "N");
        for (MELF_ApiField apiField : apiFields) {
            Object value = responseJSONBody.get(apiField.getElf_ExtName());
            if (value != null) {
                po.set_ValueOfColumn(apiField.getName(), value);
            }
        }
        po.set_ValueOfColumn("Elf_IsSent", true);
        po.set_ValueOfColumn("Elf_SentOn", new Timestamp(System.currentTimeMillis()));       
        po.saveEx();
    }
	
	private MELF_ApiField getApiField(String ApiFieldName) {
		// TODO Auto-generated method stub
		MELF_ApiField apiField = new Query(getCtx(), MELF_ApiField.Table_Name, "elf_apitemplate_id = ? and name = ?",
				get_TrxName()).setParameters(get_ID(), ApiFieldName).setOnlyActiveRecords(true).setClient_ID().first();
		return apiField;
	}


	public List<MELF_ApiField> getApiFields(String isBase, String isResponse, String isRequest) {
		// TODO Auto-generated method stub
		List<MELF_ApiField> apiFields = new Query(getCtx(), MELF_ApiField.Table_Name, "elf_apitemplate_id = ? and elf_isbase = ? and elf_isresponse = ? and elf_isrequest = ?",
				get_TrxName()).setParameters(get_ID(),isBase, isResponse, isRequest).setOnlyActiveRecords(true).setClient_ID().list();
		return apiFields;
	}
	
	private List<MELF_ApiField> getChildApiFields(int elf_parentapifield_id) {
		// TODO Auto-generated method stub
		List<MELF_ApiField> apiFields = new Query(getCtx(), MELF_ApiField.Table_Name, "elf_apitemplate_id = ? and elf_parentapifield_id = ?",
				get_TrxName()).setParameters(get_ID(),elf_parentapifield_id).setOnlyActiveRecords(true).setClient_ID().list();
		return apiFields;
	}

    /**
     * Returns a map of external field names to their resolved values using elf_expression.
     * Supports expressions like ${PO.fieldname} and ${PO2.fieldname}, where PO2 is a related PO.
     * Allows combining fields and adding other characters in the result string.
     */
    public Map<String, Object> getApiFieldsWithExpression(PO po, Map<String, Object> result) {
    	if (result == null) {
			result = new HashMap<>();
		}
        List<MELF_ApiField> apiFields = getApiFields("Y", "N", "Y");
        Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
        for (MELF_ApiField apiField : apiFields) {
            String expr = apiField.get_ValueAsString("elf_expression");
            String extName = apiField.getElf_ExtName();
            if (expr != null && !expr.isEmpty()) {
                StringBuffer sb = new StringBuffer();
                Matcher matcher = pattern.matcher(expr);
                while (matcher.find()) {
                    String ref = matcher.group(1); // e.g. PO.fieldname or PO2.fieldname
                    String[] parts = ref.split("\\.");
                    Object value = null;
                    if (parts.length == 2) {
                        String poName = parts[0];
                        String fieldName = parts[1];
                        if (poName.equals("PO")) {
                            value = po.get_Value(fieldName);
                        } else {
                            // Assume PO2 is a related PO, and its foreign key is poName.toLowerCase() + "_id"
                            String fk = poName.toLowerCase() + "_id";
                            if (po.columnExists(fk)) {
                                Object fkValue = po.get_Value(fk);
                                if (fkValue != null) {
                                    // Try to get PO class by convention: "M" + capitalize(poName)
                                    String className = "org.compiere.model.M" + poName.substring(2); // e.g. MProduct
                                    try {
                                        Class<?> clazz = Class.forName(className);
                                        PO relatedPO = (PO) clazz.getConstructor(Properties.class, int.class, String.class)
                                                .newInstance(po.getCtx(), Integer.parseInt(fkValue.toString()), po.get_TrxName());
                                        value = relatedPO.get_Value(fieldName);
                                    } catch (Exception e) {
                                        value = "";
                                    }
                                }
                            }
                        }
                    }
                    matcher.appendReplacement(sb, value != null ? value.toString() : "");
                }
                matcher.appendTail(sb);
                result.put(extName, sb.toString());
            } else {
                // Logic from getJsonRequestBody(PO po)
                if(apiField.isParent()) {
                    List<MELF_ApiField> childApiFields = getChildApiFields(apiField.get_ID());
                    JSONObject childJSONBody = new JSONObject();
                    for (MELF_ApiField childApiField : childApiFields) {
                        Object value = po.get_Value(childApiField.getName());
                        if (value != null && value.toString().startsWith("http")) {
                            String l_link = "<a href='" + value + "' target=\"_blank\">link</a>";
                            childJSONBody.put(childApiField.getElf_ExtName(), l_link);
                        } else {
                            childJSONBody.put(childApiField.getElf_ExtName(), value);
                        }
                    }
                    result.put(extName, childJSONBody);
                } else if(apiField.isElf_setDefault()) {
                    result.put(extName, apiField.getElf_DefaultValue() != null ? apiField.getElf_DefaultValue() : JSONObject.NULL);
                } else if(po.columnExists(apiField.getName())) {
                    result.put(extName, po.get_Value(apiField.getName()));
                }
            }
        }
        return result;
    }
}
