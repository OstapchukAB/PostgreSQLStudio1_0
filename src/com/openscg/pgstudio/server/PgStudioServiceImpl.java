/*
 * PostgreSQL Studio
 */
package com.openscg.pgstudio.server;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.openscg.pgstudio.client.PgStudio.DATABASE_OBJECT_TYPE;
import com.openscg.pgstudio.client.PgStudio.INDEX_TYPE;
import com.openscg.pgstudio.client.PgStudio.ITEM_OBJECT_TYPE;
import com.openscg.pgstudio.client.PgStudio.TYPE_FORM;
import com.openscg.pgstudio.client.PgStudioService;
import com.openscg.pgstudio.client.PgStudio.ITEM_TYPE;
import com.openscg.pgstudio.server.models.Columns;
import com.openscg.pgstudio.server.models.Constraints;
import com.openscg.pgstudio.server.models.Database;
import com.openscg.pgstudio.server.models.FTSConfigurations;
import com.openscg.pgstudio.server.models.ForeignTables;
import com.openscg.pgstudio.server.models.Functions;
import com.openscg.pgstudio.server.models.Indexes;
import com.openscg.pgstudio.server.models.ItemData;
import com.openscg.pgstudio.server.models.ItemMetaData;
import com.openscg.pgstudio.server.models.Monitor;
import com.openscg.pgstudio.server.models.Policies;
import com.openscg.pgstudio.server.models.Privileges;
import com.openscg.pgstudio.server.models.QueryMetaData;
import com.openscg.pgstudio.server.models.Rules;
import com.openscg.pgstudio.server.models.Schemas;
import com.openscg.pgstudio.server.models.Sequences;
import com.openscg.pgstudio.server.models.SourceCode;
import com.openscg.pgstudio.server.models.Stats;
import com.openscg.pgstudio.server.models.Tables;
import com.openscg.pgstudio.server.models.Triggers;
import com.openscg.pgstudio.server.models.Types;
import com.openscg.pgstudio.server.models.Views;
import com.openscg.pgstudio.server.models.dataimport.InsertData;
import com.openscg.pgstudio.server.models.fulltextsearch.Dictionaries;
import com.openscg.pgstudio.server.models.fulltextsearch.Parsers;
import com.openscg.pgstudio.server.util.ConnectionInfo;
import com.openscg.pgstudio.server.util.ConnectionManager;
import com.openscg.pgstudio.server.util.QueryExecutor;
import com.openscg.pgstudio.server.util.QuotingLogic;
import com.openscg.pgstudio.shared.DatabaseConnectionException;
import com.openscg.pgstudio.shared.PostgreSQLException;
import com.openscg.pgstudio.shared.dto.AlterColumnRequest;
import com.openscg.pgstudio.shared.dto.AlterDomainRequest;
import com.openscg.pgstudio.shared.dto.AlterFunctionRequest;
import com.openscg.pgstudio.shared.dto.DomainDetails;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class PgStudioServiceImpl extends RemoteServiceServlet implements
PgStudioService {

	QuotingLogic q= new QuotingLogic();

	@Override
	public void doLogout(String connectionToken, String source) throws DatabaseConnectionException	{
		/*The argument source keeps a track of the origin of the doLogout call.
		 * 	doLogout strictly works with three strings , viz:
		 * WINDOW_CLOSE : pass this when logging out due to a window/tab close action
		 * USER_INITIATED : pass this when logging out from the Disconnect button
		 * SESSION_TIMEOUT : pass this when logging out due to timeout.
		 */
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");
		ConnectionManager connMgr = new ConnectionManager();

		connMgr.closeConnection(connectionToken, clientIP, userAgent);

		if (request.getSession(false) != null)	{
			if(source.equals("WINDOW_CLOSE"))	{
				request.getSession(false).invalidate();
			}
			else
				if(source.equals("USER_INITIATED") || source.equals("SESSION_TIMEOUT"))	{
					request.getSession(false).setAttribute("dbToken", null);
					request.getSession(false).setAttribute("dbName", null);
					request.getSession(false).setAttribute("dbURL", null);
					request.getSession(false).setAttribute("username", null);
				}
		}
	}

	@Override
	public void invalidateSession()	{
		HttpServletRequest request = this.getThreadLocalRequest();  
		request.getSession(false).invalidate();
	}

	@Override
	public String getConnectionInfoMessage(String connectionToken)
			throws IllegalArgumentException, DatabaseConnectionException {

		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		Connection conn = connMgr.getConnection(connectionToken,clientIP, userAgent);

		String host = "";
		String db = "";
		String user = "";
		try {
			String url = conn.getMetaData().getURL();
			url = url.replace("jdbc:postgresql://", "");
			host = url.split(":")[0];
			db = conn.getCatalog();
			user = conn.getMetaData().getUserName();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String ret = host+ "/" + db + " as " + user;
		return ret;
	}

	@Override
	public String getList(String connectionToken, DATABASE_OBJECT_TYPE type)
			throws IllegalArgumentException, DatabaseConnectionException {

		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		Database db;

		switch (type) {
		case DATA_TYPE:
			db = new Database(connMgr.getConnection(connectionToken,clientIP, userAgent));
			return db.getDataTypes();
		case FOREIGN_SERVER:
			db = new Database(connMgr.getConnection(connectionToken,clientIP, userAgent));
			return db.getForeignServerList();
		case SCHEMA:
			Schemas schemas = new Schemas(connMgr.getConnection(connectionToken,clientIP, userAgent));			
			return schemas.getList();
		case LANGUAGE:
			db = new Database(connMgr.getConnection(connectionToken,clientIP, userAgent));
			return db.getLanguageList();
		case ROLE:
			db = new Database(connMgr.getConnection(connectionToken,clientIP, userAgent));
			return db.getRoleList();
		case DATABASE:
			db = new Database(connMgr.getConnection(connectionToken,clientIP, userAgent));
			return db.getDatabaseList();
		default:
			return "";
		}

	}

	@Override
	public String getList(String connectionToken, int schema, ITEM_TYPE type)
			throws IllegalArgumentException, DatabaseConnectionException {

		ConnectionManager connMgr = new ConnectionManager();		
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		switch (type) {
		case TABLE:
			Tables tables = new Tables(connMgr.getConnection(connectionToken,clientIP, userAgent));	
			return tables.getList(schema);
		case MATERIALIZED_VIEW:
		case VIEW:
			Views views = new Views(connMgr.getConnection(connectionToken,clientIP, userAgent));
			return views.getList(schema);
		case FOREIGN_TABLE:
			ForeignTables fTables = new ForeignTables(connMgr.getConnection(connectionToken,clientIP, userAgent));
			return fTables.getList(schema);
		case FUNCTION:
			Functions funcs = new Functions(connMgr.getConnection(connectionToken,clientIP, userAgent));
			return  funcs.getList(schema);
		case SEQUENCE:
			Sequences seqs = new Sequences(connMgr.getConnection(connectionToken,clientIP, userAgent));
			return seqs.getList(schema);
		case TYPE:
			Types types = new Types(connMgr.getConnection(connectionToken,clientIP, userAgent));
			return  types.getList(schema);
		case DICTIONARY:
			Dictionaries dictionaries = new Dictionaries(connMgr.getConnection(connectionToken, clientIP, userAgent));
			return dictionaries.getList(schema);
		case PARSER:
			Parsers parsers = new Parsers(connMgr.getConnection(connectionToken, clientIP, userAgent));
			return parsers.getList(schema);
		default:
			return "";
		}			
	}

	@Override
	public String getRangeDiffFunctionList(String connectionToken, String schema, String subType)
			throws IllegalArgumentException, DatabaseConnectionException {

		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		Functions funcs;

		funcs = new Functions(connMgr.getConnection(connectionToken,clientIP, userAgent));

		String funcList = funcs.getRangeDiffList(schema, subType);

		return funcList;
	}

	@Override
	public String getTriggerFunctionList(String connectionToken, int schema)
			throws IllegalArgumentException, DatabaseConnectionException {

		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		Functions funcs;

		funcs = new Functions(connMgr.getConnection(connectionToken,clientIP, userAgent));

		String funcList = funcs.getTriggerFunctionList(schema);

		return funcList;
	}


	@Override
	public String getItemObjectList(String connectionToken,
			long item, ITEM_TYPE type, ITEM_OBJECT_TYPE object)
			throws IllegalArgumentException, DatabaseConnectionException, PostgreSQLException {

		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		try {
			switch (object) {
			case TRIGGER:
				Triggers triggers = new Triggers(connMgr.getConnection(connectionToken,clientIP, userAgent));
				return  triggers.getList(item);
			case COLUMN:
				Columns columns = new Columns(connMgr.getConnection(connectionToken,clientIP, userAgent));
				return  columns.getList(item);
			case CONSTRAINT:
				Constraints constraints = new Constraints(connMgr.getConnection(connectionToken,clientIP, userAgent));
				return  constraints.getList(item);
			case GRANT:
				Privileges priv = new Privileges(connMgr.getConnection(connectionToken,clientIP, userAgent));
				return priv.getPrivileges(item, type);
			case INDEX:
				Indexes indexes = new Indexes(connMgr.getConnection(connectionToken,clientIP, userAgent));
				return indexes.getList(item);
			case RULE:
				Rules rules = new Rules(connMgr.getConnection(connectionToken,clientIP, userAgent));
				return rules.getList(item);
			case SOURCE:
				SourceCode sc = new SourceCode(connMgr.getConnection(connectionToken,clientIP, userAgent));
				return sc.getSourceCode(item, type);
			case STATS:
				Stats stats = new Stats(connMgr.getConnection(connectionToken,clientIP, userAgent));
				return stats.getList(item);
			case POLICY:
				Policies policies = new Policies(connMgr.getConnection(connectionToken,clientIP, userAgent));
				return policies.getList(item);
			default:
				return "";
			}
		} catch (SQLException e) {
			throw new PostgreSQLException(e.getMessage());
		}
	}

	@Override
	public String getItemMetaData(String connectionToken, 
			long item, ITEM_TYPE type)
			throws IllegalArgumentException, DatabaseConnectionException, PostgreSQLException 
	{

		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		ItemMetaData id = new ItemMetaData(connMgr.getConnection(connectionToken,clientIP, userAgent));

		try {
			return id.getMetaData(item, type);
		} catch (SQLException e) {
			throw new PostgreSQLException(e.getMessage());
		}
	}

	@Override
	public String getItemData(String connectionToken, long item, ITEM_TYPE type, int count)
			throws IllegalArgumentException, DatabaseConnectionException, PostgreSQLException 
	{

		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		ItemData id;

		id = new ItemData(connMgr.getConnection(connectionToken,clientIP, userAgent));

		try {
			return id.getData(item, type, count);
		} catch (SQLException e) {
			throw new PostgreSQLException(e.getMessage());
		}
	}

	@Override
	public String getQueryMetaData(String connectionToken, String query)
			throws IllegalArgumentException, DatabaseConnectionException {

		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		QueryMetaData id;

		id = new QueryMetaData(connMgr.getConnection(connectionToken,clientIP, userAgent));

		return id.getMetaData(query);
	}

	@Override
	public String executeQuery(String connectionToken, String query, String queryType)
			throws IllegalArgumentException, DatabaseConnectionException {

		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		QueryExecutor id;

		id = new QueryExecutor(connMgr.getConnection(connectionToken,clientIP, userAgent));

		return id.Execute(query, queryType);
	}

	@Override
	public String dropItem(String connectionToken, long item, ITEM_TYPE type, boolean cascade) 
			throws IllegalArgumentException, DatabaseConnectionException, PostgreSQLException {
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		try {
		switch (type) {
		case FOREIGN_TABLE:
			ForeignTables fTables = new ForeignTables(connMgr.getConnection(connectionToken,clientIP, userAgent));
			return fTables.drop(item, cascade);
		case TABLE:
			Tables tables = new Tables(connMgr.getConnection(connectionToken,clientIP, userAgent));		
			return tables.drop(item, cascade);
		case VIEW:
		case MATERIALIZED_VIEW:
			boolean isMaterialized = false;
			
			if (type == ITEM_TYPE.MATERIALIZED_VIEW)
				isMaterialized = true;
			
			Views views= new Views(connMgr.getConnection(connectionToken,clientIP, userAgent));			
			return views.dropView(item, cascade, isMaterialized);
		case FUNCTION:
			Functions functions = new Functions(connMgr.getConnection(connectionToken, clientIP, userAgent));
			return functions.dropFunction(item, cascade);
		case SEQUENCE:
			 Sequences sequences = new Sequences(connMgr.getConnection(connectionToken, clientIP, userAgent));
			return sequences.drop(item, cascade);
		case TYPE:
			Types types = new Types(connMgr.getConnection(connectionToken, clientIP, userAgent));
			return types.dropType(item, cascade);
		case DICTIONARY:
			Dictionaries dict = new Dictionaries(connMgr.getConnection(connectionToken, clientIP, userAgent));
			return dict.drop(item);
		default:
			return "";
		}
		} catch (SQLException e) {
			throw new PostgreSQLException(e.getMessage());			
		}
	}

	@Override
	public String dropItemObject(String connectionToken, long item, ITEM_TYPE type, String objectName, ITEM_OBJECT_TYPE objType)
			throws DatabaseConnectionException, PostgreSQLException {

		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		try {
			switch (objType) {
			case COLUMN:
				Columns columns = new Columns(connMgr.getConnection(connectionToken, clientIP, userAgent));
				return columns.drop(item, q.addQuote(objectName));

			case CONSTRAINT:
				Constraints constraints = new Constraints(connMgr.getConnection(connectionToken, clientIP, userAgent));
				return constraints.drop(item, q.addQuote(objectName));
			case INDEX:
				Indexes indexes = new Indexes(connMgr.getConnection(connectionToken, clientIP, userAgent));
				return indexes.drop(item, q.addQuote(objectName));
			case POLICY:
				Policies policies = new Policies(connMgr.getConnection(connectionToken, clientIP, userAgent));
				return policies.drop(item, q.addQuote(objectName));
			case RULE:
				Rules rules = new Rules(connMgr.getConnection(connectionToken, clientIP, userAgent));
				return rules.drop(item, q.addQuote(objectName));
			case TRIGGER:
				Triggers triggers = new Triggers(connMgr.getConnection(connectionToken, clientIP, userAgent));
				return triggers.drop(item, q.addQuote(objectName));
			default:
				return "";
			}
		} catch (SQLException e) {
			throw new PostgreSQLException(e.getMessage());
		}
	}

	@Override
	public String renameItemObject(String connectionToken, long item, ITEM_TYPE type, String objectName, ITEM_OBJECT_TYPE objType,
			String newObjectName) throws DatabaseConnectionException, PostgreSQLException {
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		try {
			switch (objType) {
			case COLUMN:
				Columns columns = new Columns(connMgr.getConnection(connectionToken, clientIP, userAgent));
				return columns.rename(item, q.addQuote(objectName), q.addQuote(newObjectName));
			case CONSTRAINT:
				Constraints constraints = new Constraints(connMgr.getConnection(connectionToken, clientIP, userAgent));
				return constraints.rename(item, q.addQuote(objectName), q.addQuote(newObjectName));
			case INDEX:
				Indexes indexes = new Indexes(connMgr.getConnection(connectionToken, clientIP, userAgent));
				return indexes.rename(item, q.addQuote(objectName), q.addQuote(newObjectName));
			case POLICY:
				Policies polices = new Policies(connMgr.getConnection(connectionToken, clientIP, userAgent));
				return polices.rename(item, q.addQuote(objectName), q.addQuote(newObjectName));
			case RULE:
				// A RULE can not be renamed so just return a blank string
				// if if get
				// here for some reason
				return "";
			case TRIGGER:
				Triggers triggers = new Triggers(connMgr.getConnection(connectionToken, clientIP, userAgent));
				return triggers.rename(item, q.addQuote(objectName), q.addQuote(newObjectName));
			default:
				return "";
			}
		} catch (SQLException e) {
			throw new PostgreSQLException(e.getMessage());
		}
	}

	@Override
	public String analyze(String connectionToken, long item, ITEM_TYPE type, boolean vacuum, boolean vacuumFull, boolean reindex) 
			throws IllegalArgumentException, DatabaseConnectionException, PostgreSQLException {
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		Tables tables;

		tables = new Tables(connMgr.getConnection(connectionToken,clientIP, userAgent));	

		try {
			return tables.analyze(item, type, vacuum, vacuumFull,reindex);
		} catch (SQLException e) {
			throw new PostgreSQLException(e.getMessage());
		}
	}

	@Override
	public String renameItem(String connectionToken, long item, ITEM_TYPE type, String newName) 
			throws IllegalArgumentException, DatabaseConnectionException, PostgreSQLException {
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		try {
			switch(type) {
			case FOREIGN_TABLE:
				ForeignTables fTables = new ForeignTables(connMgr.getConnection(connectionToken,clientIP, userAgent));
				return fTables.rename(item, type, q.addQuote(newName));
			case TABLE:
				Tables tables = new Tables(connMgr.getConnection(connectionToken,clientIP, userAgent));
				return tables.rename(item, type, q.addQuote(newName));
			case VIEW:
			case MATERIALIZED_VIEW:
				Views views = new Views(connMgr.getConnection(connectionToken,clientIP, userAgent));	
				return views.rename(item, type, newName);
			default:
				return "";			
			}
		} catch (SQLException e) {
			throw new PostgreSQLException(e.getMessage());
		}
	}

	@Override
	public String truncate(String connectionToken, long item, ITEM_TYPE type) throws DatabaseConnectionException, PostgreSQLException {
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		Tables tables = new Tables(connMgr.getConnection(connectionToken,clientIP, userAgent));

		try {
			return tables.truncate(item, type);
		} catch (SQLException e) {
			throw new PostgreSQLException(e.getMessage());
		}
	}

	@Override
	public String createTable(String connectionToken, int schema,
			String tableName, boolean unlogged, boolean temporary, String fill,
			ArrayList<String> col_list, HashMap<Integer, String> commentLog, ArrayList<String> col_index)
					throws DatabaseConnectionException, PostgreSQLException {

		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		Tables tables = new Tables(connMgr.getConnection(connectionToken,clientIP, userAgent));

		try {
			return tables.create(schema, q.addQuote(tableName), unlogged, temporary, fill, col_list, commentLog, col_index);
		} catch (SQLException e) {
			throw new PostgreSQLException(e.getMessage());
		}
	}

	@Override
	public String createTableLike(String connectionToken, int schema, String tableName, String source, boolean defaults,
			boolean constraints, boolean indexes) throws DatabaseConnectionException, PostgreSQLException {

		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		Tables tables = new Tables(connMgr.getConnection(connectionToken,clientIP, userAgent));	

		try {
			return tables.createTableLike(connectionToken, schema, tableName, source, defaults, constraints, indexes);
		} catch (SQLException e) {
			throw new PostgreSQLException(e.getMessage());
		}
	}

	@Override
	public String createView(String connectionToken, String schema,
			String viewName, String definition, String comment, boolean isMaterialized) throws DatabaseConnectionException {

		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		Views views= new Views(connMgr.getConnection(connectionToken,clientIP, userAgent));

		return views.createView(schema, q.addQuote(viewName), definition, comment, isMaterialized);

	}

	@Override
	public String createColumn(String connectionToken, long item, 
			String columnName, String datatype, String comment, boolean not_null, String defaultval) throws DatabaseConnectionException, PostgreSQLException {
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		Columns columns;

		columns = new Columns(connMgr.getConnection(connectionToken, clientIP, userAgent));

		try {
			return columns.create(item, q.addQuote(columnName), datatype,comment, not_null, defaultval);
		} catch (SQLException e) {
			throw new PostgreSQLException(e.getMessage());
		}
	}

	@Override
	public String createIndex(String connectionToken, long item, 
			String indexName, INDEX_TYPE indexType, 
			boolean isUnique, boolean isConcurrently, ArrayList<String> columnList) throws DatabaseConnectionException, PostgreSQLException {
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		Indexes indexes = new Indexes(connMgr.getConnection(connectionToken, clientIP, userAgent));

		try {
			return indexes.create(item, indexName, indexType, isUnique, isConcurrently, columnList);
		} catch (SQLException e) {
			throw new PostgreSQLException(e.getMessage());
		}
	}

	@Override
	public String createSequence(String connectionToken, int schema,
			String sequenceName, boolean temporary, String increment,
			String minValue, String maxValue, String start, int cache, boolean cycle)
			throws DatabaseConnectionException, PostgreSQLException {

		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		Sequences sequences = new Sequences(connMgr.getConnection(connectionToken,
				clientIP, userAgent));

		try {
			return sequences.create(schema, sequenceName,
					temporary, increment, minValue, maxValue, start, cache, cycle);
		} catch (SQLException e) {
			throw new PostgreSQLException(e.getMessage());
		}
	}

	@Override
	public String renameSchema(String connectionToken, String oldSchema, String schema) throws DatabaseConnectionException {
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		Schemas schemas;

		schemas = new Schemas(connMgr.getConnection(connectionToken,clientIP, userAgent));

		return schemas.renameSchema(schema, oldSchema);
	}

	@Override
	public String dropSchema(String connectionToken, String schemaName, boolean cascade) throws DatabaseConnectionException {
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		Schemas schemas;

		schemas = new Schemas(connMgr.getConnection(connectionToken,clientIP, userAgent));

		return schemas.dropSchema(schemaName, cascade);
	}

	@Override
	public String createSchema(String connectionToken, String schemaName) throws DatabaseConnectionException {
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		Schemas schemas;

		schemas = new Schemas(connMgr.getConnection(connectionToken,clientIP, userAgent));

		return schemas.createSchema(schemaName);
	}

	@Override
	public String getExplainResult(String connectionToken, String query) throws IllegalArgumentException, DatabaseConnectionException {
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		ItemData id;

		id = new ItemData(connMgr.getConnection(connectionToken,clientIP, userAgent));

		return id.getExplainResult(query);
	}

	@Override
	public String createUniqueConstraint(String connectionToken, long item, 
			String constraintName, boolean isPrimaryKey,
			ArrayList<String> columnList) throws DatabaseConnectionException, PostgreSQLException {
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		Constraints constraints;

		constraints = new Constraints(connMgr.getConnection(connectionToken,
				clientIP, userAgent));

		try {
			return constraints.createUniqueConstraint(item, constraintName, isPrimaryKey, columnList);
		} catch (SQLException e) {
			throw new PostgreSQLException(e.getMessage());
		}

	}

	@Override
	public String createCheckConstraint(String connectionToken, long item, String constraintName, String definition)
			throws DatabaseConnectionException, PostgreSQLException {
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		Constraints constraints;

		constraints = new Constraints(connMgr.getConnection(connectionToken,
				clientIP, userAgent));

		try {
			return constraints.createCheckConstraint(item, constraintName, definition);
		} catch (SQLException e) {
			throw new PostgreSQLException(e.getMessage());
		}
	}

	@Override
	public String createForeignKeyConstraint(String connectionToken,
			long item, String constraintName,
			ArrayList<String> columnList, String referenceTable,
			ArrayList<String> referenceList) throws DatabaseConnectionException, PostgreSQLException {
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		Constraints constraints;

		constraints = new Constraints(connMgr.getConnection(connectionToken,
				clientIP, userAgent));

		try {
			return constraints.createForeignKeyConstraint(item, constraintName, columnList, referenceTable,
					referenceList);
		} catch (SQLException e) {
			throw new PostgreSQLException(e.getMessage());
		}
	}

	@Override
	public String createFunction(String connectionToken, AlterFunctionRequest funcRequest)
			throws DatabaseConnectionException, PostgreSQLException {
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		Functions funcs;

		funcs = new Functions(connMgr.getConnection(connectionToken, clientIP,
				userAgent));

		try {
			return funcs.create(funcRequest);
		} catch (SQLException e) {
			throw new PostgreSQLException(e.getMessage());
		}
	}



	@Override
	public String createType(String connectionToken, String schema,
			String typeName, TYPE_FORM form, String baseType, String definition,
			ArrayList<String> attributeList) throws DatabaseConnectionException {
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		Types types;

		types = new Types(connMgr.getConnection(connectionToken, clientIP,
				userAgent));

		return types.createType(connectionToken, schema, typeName, form,
				baseType, definition, attributeList);
	}

	@Override
	public String createForeignTable(String connectionToken, String schema,
			String tableName, String server,
			ArrayList<String> columns, HashMap<Integer, String> comments, ArrayList<String> options)
					throws IllegalArgumentException, DatabaseConnectionException {

		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		ForeignTables tables;

		tables = new ForeignTables(connMgr.getConnection(connectionToken,clientIP, userAgent));

		return tables.createForeignTable(connectionToken, schema, tableName, server, columns, comments, options);
	}

	@Override
	public String createRule(String connectionToken, long item, ITEM_TYPE type,
			String ruleName, String event, String ruleType, String definition)
					throws DatabaseConnectionException, PostgreSQLException {
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		Rules rules;

		rules = new Rules(connMgr.getConnection(connectionToken, clientIP,
				userAgent));

		try {
			return rules.createRule(item, type, ruleName, event, ruleType,
					definition);
		} catch (SQLException e) {
			throw new PostgreSQLException(e.getMessage());
		}
	}

	@Override
	public String createTrigger(String connectionToken, long item, ITEM_TYPE type,
			String triggerName,
			String event, String triggerType, String forEach,
			String function) throws DatabaseConnectionException, PostgreSQLException {
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		Triggers triggers = new Triggers(connMgr.getConnection(connectionToken,
				clientIP, userAgent));

		try {
			return triggers.createTrigger(item, type, triggerName, event,
					triggerType, forEach, function);
		} catch (SQLException e) {
			throw new PostgreSQLException(e.getMessage());
		}
	}

	@Override
	public String revoke(String connectionToken, long item, ITEM_TYPE type,
			String privilege, String grantee, boolean cascade)
					throws DatabaseConnectionException, PostgreSQLException {
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		Privileges priv = new Privileges(connMgr.getConnection(connectionToken,
				clientIP, userAgent));
		try {
			return priv.revoke(item, type, privilege, grantee, cascade);
		} catch (SQLException e) {
			throw new PostgreSQLException(e.getMessage());
		}
	}

	@Override
	public String grant(String connectionToken, long item, ITEM_TYPE type,
			ArrayList<String> privileges, String grantee)
					throws DatabaseConnectionException, PostgreSQLException {
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		Privileges priv = new Privileges(connMgr.getConnection(connectionToken,
				clientIP, userAgent));
		try {
			return priv.grant(item, type, privileges, grantee);
		} catch (SQLException e) {
			throw new PostgreSQLException(e.getMessage());
		}

	}

	/***********************************************************************************************/

	@Override
	public String refreshMaterializedView(String connectionToken,
			String schema, String viewName) throws DatabaseConnectionException {

		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		Views views = new Views(connMgr.getConnection(connectionToken,
				clientIP, userAgent));

		return views.refreshMaterializedView(schema, q.addQuote(viewName));
	}

	@Override
	public String getActivity(String connectionToken) throws DatabaseConnectionException {

		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		Monitor monitor = new Monitor(connMgr.getConnection(connectionToken,
				clientIP, userAgent));

		return monitor.getActivity();
	}

	@Override
	public String configureRowSecurity(String connectionToken, long item, boolean rowSecurity, boolean forceRowSecurity)
			throws DatabaseConnectionException, PostgreSQLException {

		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		Policies policies = new Policies(connMgr.getConnection(connectionToken, clientIP, userAgent));

		try {
			return policies.configureRowSecurity(item, rowSecurity, forceRowSecurity);
		} catch (SQLException e) {
			throw new PostgreSQLException(e.getMessage());
		}

	}

	@Override
	public String createPolicy(String connectionToken, long item, String policyName, String cmd, String role, String using, String withCheck)
			throws DatabaseConnectionException, PostgreSQLException {
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		Policies policies;

		policies = new Policies(connMgr.getConnection(connectionToken, clientIP, userAgent));

		try {
			return policies.create(item, q.addQuote(policyName), cmd, role, using, withCheck);
		} catch (SQLException e) {
			throw new PostgreSQLException(e.getMessage());
		}
	}

	@Override
	public String alterFunction(String connectionToken, AlterFunctionRequest alterFunctionRequest)
			throws Exception {
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		Functions funcs;

		System.out.println("Received : " + alterFunctionRequest);
		funcs = new Functions(connMgr.getConnection(connectionToken, clientIP,
				userAgent));

		try {
			return funcs.alter(alterFunctionRequest);
		} catch (Exception e) {
			throw new PostgreSQLException(e.getMessage());
		}

	}

	@Override
	public String incrementValue(String connectionToken, int schema, String sequenceName) throws DatabaseConnectionException, PostgreSQLException {

		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");
		Connection conn = connMgr.getConnection(connectionToken,clientIP, userAgent);

		Sequences sequences = new Sequences(conn);
		try {
			return sequences.incrementValue(schema, sequenceName);
		} catch (SQLException e) {
			throw new PostgreSQLException(e.getMessage());
		}
	}

	@Override
	public String alterSequence(String connectionToken, int schema, String sequenceName, String increment, String minValue,
			String maxValue, String start, int cache, boolean cycle) throws DatabaseConnectionException, PostgreSQLException {

		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		Sequences sequences = new Sequences(connMgr.getConnection(connectionToken,clientIP, userAgent));

		try {
			return sequences.alter(schema, sequenceName,
					increment, minValue, maxValue, start, cache, cycle);
		} catch (SQLException e) {
			throw new PostgreSQLException(e.getMessage());
		}

	}

	@Override
	public String changeSequenceValue(String connectionToken, int schema, String sequenceName, String value) throws DatabaseConnectionException, PostgreSQLException {
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		Sequences sequences = new Sequences(connMgr.getConnection(connectionToken,clientIP, userAgent));

		try {
			return sequences.changeValue(schema, sequenceName, value);
		} catch (SQLException e) {
			throw new PostgreSQLException(e.getMessage());
		}		
	}

	@Override
	public String restartSequence(String connectionToken, int schema, String sequenceName)
			throws DatabaseConnectionException, PostgreSQLException {
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		Sequences sequences = new Sequences(connMgr.getConnection(connectionToken,clientIP, userAgent));

		try {
			return sequences.restart(schema, sequenceName);
		} catch (SQLException e) {
			throw new PostgreSQLException(e.getMessage());
		}
	}

	@Override
	public String resetSequence(String connectionToken, int schema, String sequenceName) throws DatabaseConnectionException, PostgreSQLException {

		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		Sequences sequences = new Sequences(connMgr.getConnection(connectionToken,clientIP, userAgent));

		try {
			return sequences.reset(schema, sequenceName);
		} catch (SQLException e) {
			throw new PostgreSQLException(e.getMessage());
		}

	}

	@Override
	public String getLanguageFullList(String connectionToken, DATABASE_OBJECT_TYPE type)
			throws IllegalArgumentException, DatabaseConnectionException {

		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		Database db;
		db = new Database(connMgr.getConnection(connectionToken,clientIP, userAgent));
		return db.getLanguageFullList();
	}

	@Override
	public String getFunctionFullList(String connectionToken, int schema, ITEM_TYPE type)
			throws IllegalArgumentException, DatabaseConnectionException {

		ConnectionManager connMgr = new ConnectionManager();		
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		Functions funcs = new Functions(connMgr.getConnection(connectionToken,clientIP, userAgent));
		return  funcs.getFullList(schema);

	}

	@Override
	public String fetchDictionaryTemplates(String connectionToken)
			throws DatabaseConnectionException, PostgreSQLException {
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		Dictionaries dict = new Dictionaries(connMgr.getConnection(connectionToken, clientIP, userAgent));

		try {
			return dict.getTemplates();
		} catch (Exception e) {
			throw new PostgreSQLException(e.getMessage());
		}
	}

	@Override
	public String addDictionary(String connectionToken, int schema, String dictName, String template, String option,
			String comment) throws DatabaseConnectionException, PostgreSQLException {
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		Dictionaries dict = new Dictionaries(connMgr.getConnection(connectionToken, clientIP, userAgent));

		try {
			Schemas s = new Schemas(connMgr.getConnection(connectionToken, clientIP, userAgent));
			String schemaName = s.getName(schema);
			return dict.create(schemaName, dictName, template, option, comment);
		} catch (Exception e) {
			throw new PostgreSQLException(e.getMessage());
		}
	}

	@Override
	public String createFTSConfiguration(String connectionToken, String schemaName, String configurationName, String templateName, String parserName, String comments)
			throws DatabaseConnectionException, PostgreSQLException {
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		FTSConfigurations ftsConfigurations = new FTSConfigurations(connMgr.getConnection(connectionToken,clientIP, userAgent));

		try {
			return ftsConfigurations.createConfig(schemaName, configurationName, templateName, parserName, comments);
		} catch (Exception e) {
			throw new PostgreSQLException(e.getMessage());
		}
	}

	@Override
	public String getFTSTemplatesList(String connectionToken) throws DatabaseConnectionException, PostgreSQLException {

		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		FTSConfigurations ftsConfigurations = new FTSConfigurations(connMgr.getConnection(connectionToken,clientIP, userAgent));
		try {
			return ftsConfigurations.getTemplatesList();
		} catch (Exception e) {
			throw new PostgreSQLException(e.getMessage());
		}

		//return parsersListStr;
	}

	@Override
	public String getFTSParsersList(String connectionToken) throws DatabaseConnectionException, PostgreSQLException {

		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		FTSConfigurations ftsConfigurations = new FTSConfigurations(connMgr.getConnection(connectionToken,clientIP, userAgent));
		try {
			return ftsConfigurations.getParsersList();
		} catch (Exception e) {
			throw new PostgreSQLException(e.getMessage());
		}

		//return parsersListStr;
	}

	@Override
	public String alterFTSConfiguration(String connectionToken, String schemaName, String configurationName, String comments)
			throws DatabaseConnectionException, PostgreSQLException {
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");
		FTSConfigurations ftsConfigurations = new FTSConfigurations(connMgr.getConnection(connectionToken,clientIP, userAgent));
		try {
			return ftsConfigurations.alterConfig(schemaName, configurationName, comments);
		} catch (Exception e) {
			throw new PostgreSQLException(e.getMessage());
		}
	}

	@Override
	public String dropFTSConfiguration(String connectionToken, String schemaName, String configurationName, boolean cascade)
			throws DatabaseConnectionException, PostgreSQLException {
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		FTSConfigurations ftsConfigurations = new FTSConfigurations(connMgr.getConnection(connectionToken,clientIP, userAgent));

		try {
			return ftsConfigurations.dropConfig(schemaName, configurationName, cascade);
		} catch (Exception e) {
			throw new PostgreSQLException(e.getMessage());
		}
	}

	@Override
	public String getFTSConfigurations(String connectionToken, String schemaName)
			throws DatabaseConnectionException, PostgreSQLException {
		System.out.println("getFTSConfigurations is called....");
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		FTSConfigurations ftsConfigurations = new FTSConfigurations(connMgr.getConnection(connectionToken,clientIP, userAgent));
		try {
			return ftsConfigurations.getFTSConfigurations(schemaName);
		} catch (Exception e) {
			throw new PostgreSQLException(e.getMessage());
		}
	}

	@Override
	public String fetchDictionaryDetails(String connectionToken, int schema, long id)
			throws DatabaseConnectionException, PostgreSQLException {
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		Dictionaries dict = new Dictionaries(connMgr.getConnection(connectionToken, clientIP, userAgent));

		try {
			return dict.get(schema, id);
		} catch (SQLException e) {
			throw new PostgreSQLException(e.getMessage());
		}
	}

	@Override
	public String alterDictionary(String connectionToken, int schema, String dictName, String newDictName,
			String options, String comments) throws DatabaseConnectionException, PostgreSQLException {
		System.out.println("Received request");
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		Dictionaries dict = new Dictionaries(connMgr.getConnection(connectionToken, clientIP, userAgent));

		try {
			return dict.alter(dictName, newDictName, options, comments);
		} catch (Exception e) {
			throw new PostgreSQLException(e.getMessage());
		}
	}

	@Override
	public String createFTSMapping(String connectionToken, String schemaName, String configurationName, String tokenType, String dictionary)
			throws DatabaseConnectionException, PostgreSQLException {
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		FTSConfigurations ftsConfigurations = new FTSConfigurations(connMgr.getConnection(connectionToken,clientIP, userAgent));

		try {
			return ftsConfigurations.createMapping(schemaName, configurationName, tokenType, dictionary);
		} catch (Exception e) {
			throw new PostgreSQLException(e.getMessage());
		}
	}

	@Override
	public String getFTSTokensList(String connectionToken) throws DatabaseConnectionException, PostgreSQLException {

		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		FTSConfigurations ftsConfigurations = new FTSConfigurations(connMgr.getConnection(connectionToken,clientIP, userAgent));
		try {
			return ftsConfigurations.getTokensList();
		} catch (Exception e) {
			throw new PostgreSQLException(e.getMessage());
		}

		//return parsersListStr;
	}

	@Override
	public String getFTSDictionariesList(String connectionToken) throws DatabaseConnectionException, PostgreSQLException {

		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		FTSConfigurations ftsConfigurations = new FTSConfigurations(connMgr.getConnection(connectionToken,clientIP, userAgent));
		try {
			return ftsConfigurations.getDictionariesList();
		} catch (Exception e) {
			throw new PostgreSQLException(e.getMessage());
		}

		//return parsersListStr;
	}

	@Override
	public String alterFTSMapping(String connectionToken, String schemaName, String configurationName, String tokenType, String oldDict, String newDict)
			throws DatabaseConnectionException, PostgreSQLException {
		System.out.println("alterFTSConfiguration.....start");
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");
		System.out.println("alterFTSConfiguration.....start11111");
		FTSConfigurations ftsConfigurations = new FTSConfigurations(connMgr.getConnection(connectionToken,clientIP, userAgent));
		System.out.println("alterFTSConfiguration.....start22222");
		try {
			return ftsConfigurations.alterMapping(schemaName, configurationName, tokenType, oldDict, newDict);
		} catch (Exception e) {
			throw new PostgreSQLException(e.getMessage());
		}
	}

	@Override
	public String dropFTSMapping(String connectionToken, String schemaName, String configurationName, String token)
			throws DatabaseConnectionException, PostgreSQLException {
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		FTSConfigurations ftsConfigurations = new FTSConfigurations(connMgr.getConnection(connectionToken,clientIP, userAgent));

		try {
			return ftsConfigurations.dropMapping(schemaName, configurationName, token);
		} catch (Exception e) {
			throw new PostgreSQLException(e.getMessage());
		}
	}

	@Override
	public String getFTSConfigurationDetails(String connectionToken, String schemaName, String configName)
			throws DatabaseConnectionException, PostgreSQLException {
		System.out.println("getFTSConfigurations is called....");
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		FTSConfigurations ftsConfigurations = new FTSConfigurations(connMgr.getConnection(connectionToken,clientIP, userAgent));
		try {
			return ftsConfigurations.getFTSConfigurationDetails(schemaName, configName);
		} catch (Exception e) {
			throw new PostgreSQLException(e.getMessage());
		}
	}

	@Override
	public String importData(String connectionToken, List<String> columnList, List<ArrayList<String>> dataRows, int schema,
			String tableId, String tableName) throws DatabaseConnectionException, PostgreSQLException {

		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");
		Connection conn = connMgr.getConnection(connectionToken,clientIP, userAgent);

		InsertData importData = new InsertData(conn);

		Schemas s = new Schemas(conn);

		try {
			String schemaName = s.getName(schema);
			return importData.insert(columnList, dataRows, schemaName, tableId, tableName);
		} catch (Exception e) {
			throw new PostgreSQLException(e.getMessage());
		}
	}

	@Override
	public String dropItemData(String connectionToken, int schema, String tableName)
			throws IllegalArgumentException, DatabaseConnectionException, PostgreSQLException {
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");

		ItemData itemData = new ItemData(connMgr.getConnection(connectionToken, clientIP, userAgent));

		return itemData.dropItemData(schema, tableName);

	}

	@Override
	public void connectToDatabase(String connectionToken, String databaseName) throws DatabaseConnectionException, PostgreSQLException{

		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");
		try {	
			connMgr.connectedToDatabase(connectionToken, databaseName, clientIP, userAgent);
		} catch (DatabaseConnectionException e) 
		{
			throw new PostgreSQLException(e.getMessage());
		}
	}
	
	@Override
	public DomainDetails fetchDomainDetails(String connectionToken, long item)
			throws DatabaseConnectionException, PostgreSQLException {
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest httpRequest = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(httpRequest);
		String userAgent = httpRequest.getHeader("User-Agent");

		Types types;

		types = new Types(connMgr.getConnection(connectionToken, clientIP,
				userAgent));

		try {
			return types.getDomainDetails(item);
		} catch (SQLException e) {
			throw new PostgreSQLException(e.getMessage());
		}
	}
	
	@Override
	public String addCheck(String connectionToken, int schema, String domainName, String checkName, String expression)
			throws DatabaseConnectionException, PostgreSQLException {
		
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");
		Connection conn = connMgr.getConnection(connectionToken,clientIP, userAgent);
		
		Types types = new Types(conn);
		
		try {
			return types.addCheck(schema, domainName, checkName, expression);
		} catch (SQLException e) {
			throw new PostgreSQLException(e.getMessage());
		}
	}


	@Override
	public String dropCheck(String connectionToken, int schema, String domainName, String checkName)
			throws DatabaseConnectionException, PostgreSQLException {
		
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");
		Connection conn = connMgr.getConnection(connectionToken,clientIP, userAgent);
		
		Types types = new Types(conn);
		
		try {
			return types.dropCheck(schema, domainName, checkName);
		} catch (SQLException e) {
			throw new PostgreSQLException(e.getMessage());
		}
	}
	
	@Override
	public String alterDomain(String connectionToken, int schema, AlterDomainRequest request)
			throws DatabaseConnectionException, PostgreSQLException {
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest httpRequest = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(httpRequest);
		String userAgent = httpRequest.getHeader("User-Agent");

		Types types;

		types = new Types(connMgr.getConnection(connectionToken, clientIP,
				userAgent));

		try {
			return types.alter(connectionToken, schema, request);
		} catch (SQLException e) {
			throw new PostgreSQLException(e.getMessage());
		}
	}

	@Override
	public String createItemData(String connectionToken, int schema, String tableName, ArrayList<String>colNames, ArrayList<String> values) throws DatabaseConnectionException, PostgreSQLException {
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");
		
		ItemData itemData = new ItemData(connMgr.getConnection(connectionToken, clientIP, userAgent));
		
		try {
			return itemData.createData(schema, tableName, colNames, values);
		} catch (SQLException e) {
			throw new PostgreSQLException(e.getMessage());
		}
	}

	
	@Override
	public String alterColumn(String connectionToken, long item, AlterColumnRequest alterCommand)
			throws DatabaseConnectionException, PostgreSQLException {
		ConnectionManager connMgr = new ConnectionManager();
		HttpServletRequest request = this.getThreadLocalRequest();  

		String clientIP = ConnectionInfo.remoteAddr(request);
		String userAgent = request.getHeader("User-Agent");
		
		Columns columns;
		
		columns = new Columns(connMgr.getConnection(connectionToken, clientIP, userAgent));
		
		try {
			return columns.alter(item, alterCommand);
		} catch (SQLException e) {
			throw new PostgreSQLException(e.getMessage());
		}
	}
	
}
