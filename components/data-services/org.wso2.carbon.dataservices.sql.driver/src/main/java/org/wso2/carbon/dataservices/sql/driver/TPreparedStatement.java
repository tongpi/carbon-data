/*
 *  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.wso2.carbon.dataservices.sql.driver.parser.Parser;
import org.wso2.carbon.dataservices.sql.driver.parser.ParserUtil;
import org.wso2.carbon.dataservices.sql.driver.query.ParamInfo;
import org.wso2.carbon.dataservices.sql.driver.query.Query;
import org.wso2.carbon.dataservices.sql.driver.query.QueryFactory;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;
import java.util.Queue;

public class TPreparedStatement extends TStatement implements PreparedStatement {

    private ParamInfo[] parameters;

    private boolean isClosed = false;

    private String sql;

    private Queue<String> processedTokens;

    private String queryType;

    private ResultSet currentResultSet;

    public TPreparedStatement(Connection connection, String sql) throws SQLException {
        super(connection);
        this.sql = sql;
        this.parameters = ParserUtil.extractParameters(getSql());
        this.queryType = ParserUtil.extractFirstKeyword(getSql());
        if (getQueryType() != null) {
            this.queryType = getQueryType().toUpperCase();
        }
        this.processedTokens = Parser.parse(sql, getQueryType());
    }

    public TPreparedStatement() {
    }

    public ResultSet executeQuery() throws SQLException {
        determineConnectionState();
        synchronized (getConnection()) {
            Query query = QueryFactory.createQuery(this);
            currentResultSet = query.executeQuery();
            return currentResultSet;
        }
    }

    public int executeUpdate() throws SQLException {
        determineConnectionState();
        synchronized (getConnection()) {
            Query query = QueryFactory.createQuery(this);
            return query.executeUpdate();
        }
    }

    public boolean execute() throws SQLException {
        determineConnectionState();
        synchronized (getConnection()) {
            Query query = QueryFactory.createQuery(this);
            return query.execute();
        }
    }

    private ParamInfo getParameter(int parameterIndex) throws SQLException {
        determineConnectionState();
        if (parameterIndex > this.getParameters().length || parameterIndex < 0) {
            throw new SQLException("无效的参数索引 '" + parameterIndex + "'");
        }
        return parameters[parameterIndex - 1];
    }

    private void setParameter(int parameterIndex, Object value,
                              int targetSQLType) throws SQLException {
        ParamInfo param = this.getParameter(parameterIndex);
        if (param != null) {
            param.setOrdinal(parameterIndex - 1);
            param.setSqlType(targetSQLType);
            param.setValue(value);
        }
    }

    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        this.setParameter(parameterIndex, null, sqlType);
    }

    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        this.setParameter(parameterIndex, x, Types.BOOLEAN);
    }

    public void setByte(int parameterIndex, byte x) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持数据类型'byte'");
    }

    public void setShort(int parameterIndex, short x) throws SQLException {
        this.setParameter(parameterIndex, x, Types.INTEGER);
    }

    public void setInt(int parameterIndex, int x) throws SQLException {
        this.setParameter(parameterIndex, x, Types.INTEGER);
    }

    public void setLong(int parameterIndex, long x) throws SQLException {
        this.setParameter(parameterIndex, x, Types.LONGNVARCHAR);
    }

    public void setFloat(int parameterIndex, float x) throws SQLException {
        this.setParameter(parameterIndex, x, Types.FLOAT);
    }

    public void setDouble(int parameterIndex, double x) throws SQLException {
        this.setParameter(parameterIndex, x, Types.DOUBLE);
    }

    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        this.setParameter(parameterIndex, x, Types.BIGINT);
    }

    public void setString(int parameterIndex, String x) throws SQLException {
        this.setParameter(parameterIndex, x, Types.VARCHAR);
    }

    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持数据类型'byte[]'");
    }

    public void setDate(int parameterIndex, Date x) throws SQLException {
        throw new SQLFeatureNotSupportedException(" 不支持数据类型'Date'");
    }

    public void setTime(int parameterIndex, Time x) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持数据类型 'Time' ");
    }

    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持数据类型 'TimeStamp'");
    }

    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持数据类型");
    }

    public void setUnicodeStream(int parameterIndex, InputStream x,
                                 int length) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持数据类型");
    }

    public void setBinaryStream(int parameterIndex, InputStream x,
                                int length) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持数据类型");
    }

    public void clearParameters() throws SQLException {
    }

    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
        this.setParameter(parameterIndex, x, targetSqlType);
    }

    public void setObject(int parameterIndex, Object x) throws SQLException {
        this.setParameter(parameterIndex, x, -1);
    }

    public void addBatch() throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持批量添加");
    }

    public void setCharacterStream(int parameterIndex, Reader reader,
                                   int length) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持数据类型");
    }

    public void setRef(int parameterIndex, Ref x) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持数据类型 'Ref' ");
    }

    public void setBlob(int parameterIndex, Blob x) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持数据类型 'Blob' ");
    }

    public void setClob(int parameterIndex, Clob x) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持数据类型 'Clob' ");
    }

    public void setArray(int parameterIndex, Array x) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持数据类型 'Array' ");
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        return null;
    }

    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持数据类型 'Date' ");
    }

    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持数据类型 'Time' ");
    }

    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持数据类型 'TimeStamp' ");
    }

    public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持数据类型 'Null' ");
    }

    public void setURL(int parameterIndex, URL x) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持数据类型 'URL' ");
    }

    public ParameterMetaData getParameterMetaData() throws SQLException {
        determineConnectionState();
        return null;
    }

    public void setRowId(int parameterIndex, RowId x) throws SQLException {

    }

    public void setNString(int parameterIndex, String value) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持数据类型 'NString'");
    }

    public void setNCharacterStream(int parameterIndex, Reader value,
                                    long length) throws SQLException {

        throw new SQLFeatureNotSupportedException("不支持数据类型 'NCharacterStream' ");
    }

    public void setNClob(int parameterIndex, NClob value) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持数据类型 'NClob' ");
    }

    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持数据类型 'Clob' ");
    }

    public void setBlob(int parameterIndex, InputStream inputStream,
                        long length) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持数据类型 'Blob' ");
    }

    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持数据类型 'NClob'");
    }

    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持数据类型 'SQLXML' ");
    }

    public void setObject(int parameterIndex, Object x, int targetSqlType,
                          int scaleOrLength) throws SQLException {
        this.setParameter(parameterIndex, x, targetSqlType);
    }

    public void setAsciiStream(int parameterIndex, InputStream x,
                               long length) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持数据类型");
    }

    public void setBinaryStream(int parameterIndex, InputStream x,
                                long length) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持数据类型");
    }

    public void setCharacterStream(int parameterIndex, Reader reader,
                                   long length) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持数据类型");
    }

    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持数据类型");
    }

    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持数据类型");
    }

    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持数据类型");
    }

    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持数据类型");
    }

    public void setClob(int parameterIndex, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持数据类型 'Clob' ");
    }

    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持数据类型 'Blob' ");
    }

    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持数据类型 'NClob' ");
    }

    public synchronized void close() throws SQLException {
        if (this.isClosed()) {
            throw new SQLException("声明已经结束");
        }
        isClosed = true;
    }

    public ParamInfo[] getParameters() {
        return parameters;
    }

    public Queue<String> getProcessedTokens() {
        return processedTokens;
    }

    public String getSql() {
        return sql;
    }

    public boolean isClosed() throws SQLException {
        return isClosed;
    }

    public String getQueryType() {
        return queryType;
    }

    public ResultSet getCurrentResultSet() {
        return currentResultSet;
    }

    private void determineConnectionState() throws SQLException {
        if (isClosed()) {
            throw new SQLException("连接已关闭");
        }
    }

}
