/*
 *  Copyright (c) 2005-2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.dataservices.sql.driver;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.wso2.carbon.dataservices.sql.driver.processor.reader.DataTable;

/**
 * This class represents a custom data source connection for the SQL parser.
 */
public class TCustomConnection extends TConnection {

	public static final String CUSTOM_DATASOURCE = "__CUSTOM_DATASOURCE__";
	
	private CustomDataSource dataSource;
	
	public TCustomConnection(Properties props) throws SQLException {
		super(props);
		this.dataSource = (CustomDataSource) props.get(CUSTOM_DATASOURCE);
		if (this.dataSource == null) {
			throw new SQLException("自定义数据源对象在属性中不可用");
		}
	}
	
    public CustomDataSource getDataSource() {
		return dataSource;
	}

	public Statement createStatement(String sql) throws SQLException {
        return new TPreparedStatement(this, sql);
    }

    @Override
    public Statement createStatement() throws SQLException {
        return new TPreparedStatement();
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return new TPreparedStatement(this, sql);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持CallableStatements");
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType,
                                              int resultSetConcurrency) throws SQLException {
        return new TPreparedStatement(this, sql);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType,
                                         int resultSetConcurrency) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持CallableStatements");
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType,
                                              int resultSetConcurrency,
                                              int resultSetHoldability) throws SQLException {
        return new TPreparedStatement(this, sql);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
                                         int resultSetHoldability) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持CallableStatements");
    }

    @Override
    public PreparedStatement prepareStatement(String sql,
                                              int autoGeneratedKeys) throws SQLException {
        return new TPreparedStatement(this, sql);
    }

    @Override
    public PreparedStatement prepareStatement(String sql,
                                              int[] columnIndexes) throws SQLException {
        return new TPreparedStatement(this, sql);
    }

    @Override
    public PreparedStatement prepareStatement(String sql,
                                              String[] columnNames) throws SQLException {
        return null;  
    }
    
    /**
     * This interface represents a custom data source definition.
     */
    public static interface CustomDataSource {
    	
    	void init(Properties props) throws SQLException;
    	
    	Set<String> getDataTableNames() throws SQLException;
    	
    	DataTable getDataTable(String name) throws SQLException;
    	
    	void createDataTable(String name, Map<String, Integer> columns) throws SQLException;
    	
    	void dropDataTable(String name) throws SQLException;
    	
    }

}
