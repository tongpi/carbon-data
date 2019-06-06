/*
 *  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import com.google.common.io.ByteStreams;
import com.google.gdata.client.spreadsheet.*;
import com.google.gdata.data.spreadsheet.CellEntry;
import com.google.gdata.data.spreadsheet.CellFeed;
import com.google.gdata.data.spreadsheet.ListFeed;
import com.google.gdata.data.spreadsheet.SpreadsheetEntry;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.data.spreadsheet.WorksheetFeed;
import org.apache.commons.codec.binary.Base64;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.dataservices.sql.driver.internal.SQLDriverDSComponent;
import org.wso2.carbon.dataservices.sql.driver.parser.Constants;
import org.wso2.carbon.dataservices.sql.driver.processor.reader.DataTable;
import org.wso2.carbon.dataservices.sql.driver.query.ColumnInfo;
import org.wso2.carbon.dataservices.sql.driver.query.ParamInfo;
import org.wso2.carbon.dataservices.sql.driver.query.QueryFactory;
import org.wso2.carbon.dataservices.sql.driver.util.WorkBookOutputWriter;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.io.*;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TDriverUtil {

    private static List<String> driverProperties = new ArrayList<String>();
    public static final java.lang.String GOV_REGISTRY_PATH_PREFIX = "gov:";
    public static final java.lang.String CONF_REGISTRY_PATH_PREFIX = "conf:";

    static {
        driverProperties.add(Constants.DRIVER_PROPERTIES.FILE_PATH);
        driverProperties.add(Constants.DRIVER_PROPERTIES.SHEET_NAME);
        driverProperties.add(Constants.DRIVER_PROPERTIES.VISIBILITY);
        driverProperties.add(Constants.DRIVER_PROPERTIES.HAS_HEADER);
        driverProperties.add(Constants.DRIVER_PROPERTIES.USER);
        driverProperties.add(Constants.DRIVER_PROPERTIES.PASSWORD);
        driverProperties.add(Constants.DRIVER_PROPERTIES.DATA_SOURCE_TYPE);
        driverProperties.add(Constants.DRIVER_PROPERTIES.MAX_COLUMNS);
        driverProperties.add(Constants.GSPREAD_PROPERTIES.CLIENT_ID);
        driverProperties.add(Constants.GSPREAD_PROPERTIES.CLIENT_SECRET);
        driverProperties.add(Constants.GSPREAD_PROPERTIES.REFRESH_TOKEN);
    }

    public static List<String> getAvailableDriverProperties() {
        return driverProperties;
    }

    public static ColumnInfo[] getHeaders(Connection connection,
                                          String tableName) throws SQLException {
        if (!(connection instanceof TConnection)) {
            throw new SQLException("连接类型无效");
        }
        String connectionType = ((TConnection) connection).getType();
        QueryFactory.QueryTypes type =
                QueryFactory.QueryTypes.valueOf(connectionType.toUpperCase());
        switch (type) {
            case EXCEL:
                return getExcelHeaders(connection, tableName);
            case GSPREAD:
                return getGSpreadHeaders(connection, tableName);
            case CUSTOM:
                return getCustomHeaders(connection, tableName);
            default:
                throw new SQLException("查询类型无效: " + type);
        }
    }

    private static ColumnInfo[] getExcelHeaders(Connection connection,
                                                String tableName) throws SQLException {
        List<ColumnInfo> columns = new ArrayList<ColumnInfo>();
        if (!(connection instanceof TExcelConnection)) {
            throw new SQLException("连接类型无效");
        }
        Workbook workbook = ((TExcelConnection) connection).getWorkbook();
        Sheet sheet = workbook.getSheet(tableName);
        if (sheet == null) {
            throw new SQLException("表: '" + tableName + "' 不存在");
        }
        Iterator<Cell> cellItr = sheet.getRow(0).cellIterator();
        while (cellItr.hasNext()) {
            Cell header = cellItr.next();
            ColumnInfo column = new ColumnInfo(header.getStringCellValue());
            column.setTableName(tableName);
            column.setSqlType(header.getCellType());
            column.setId(header.getColumnIndex());

            columns.add(column);
        }
        return columns.toArray(new ColumnInfo[columns.size()]);
    }

    private static ColumnInfo[] getGSpreadHeaders(Connection connection,
                                                  String sheetName) throws SQLException {
        WorksheetEntry currentWorksheet;
        List<ColumnInfo> columns = new ArrayList<ColumnInfo>();

        if (!(connection instanceof TGSpreadConnection)) {
            throw new SQLException("连接类型无效");
        }
        currentWorksheet = getCurrentWorkSheetEntry((TGSpreadConnection) connection, sheetName);
        if (currentWorksheet == null) {
            throw new SQLException("表名为: '" + sheetName + "' 不存在");
        }
        CellFeed cellFeed = getGSpreadCellFeed((TGSpreadConnection) connection, currentWorksheet);
        for (CellEntry cell : cellFeed.getEntries()) {
            if (!getCellPosition(cell.getId()).startsWith("R1")) {
                break;
            }
            ColumnInfo column =
                    new ColumnInfo(cell.getTextContent().getContent().getPlainText());
            column.setTableName(sheetName);
            column.setSqlType(cell.getContent().getType());
            column.setId(getColumnIndex(cell.getId()) - 1);
            columns.add(column);
        }
        return columns.toArray(new ColumnInfo[columns.size()]);
    }

    public static WorksheetEntry getCurrentWorkSheetEntry(TGSpreadConnection connection,
                                                          String sheetName) throws SQLException {
        SpreadsheetEntry spreadsheetEntry = connection.getSpreadSheetFeed().getEntries().get(0);
        WorksheetQuery worksheetQuery =
                TDriverUtil.createWorkSheetQuery(spreadsheetEntry.getWorksheetFeedUrl());
        WorksheetFeed worksheetFeed = connection.getFeedProcessor().getFeed(worksheetQuery,
                                                                            WorksheetFeed.class);
        for (WorksheetEntry entry : worksheetFeed.getEntries()) {
            if (sheetName.equals(entry.getTitle().getPlainText())) {
                return entry;
            }
        }
        return null;
    }

    public static CellFeed getGSpreadCellFeed(TGSpreadConnection connection,
                                       WorksheetEntry currentWorkSheet) throws SQLException {
        CellQuery cellQuery = new CellQuery(currentWorkSheet.getCellFeedUrl());
        return connection.getFeedProcessor().getFeed(cellQuery, CellFeed.class);
    }

    public static ListFeed getListFeed(TGSpreadConnection connection,
                                       WorksheetEntry currentWorkSheet) throws SQLException {
        ListQuery listQuery = new ListQuery(currentWorkSheet.getListFeedUrl());
        return connection.getFeedProcessor().getFeed(listQuery, ListFeed.class);
    }

    private static ColumnInfo[] getCustomHeaders(Connection connection,
            String sheetName) throws SQLException {
    	DataTable table = ((TCustomConnection) connection).getDataSource().getDataTable(sheetName);
    	return table.getHeaders();
    }

    public static int getColumnIndex(String id) {
        String tmp = getCellPosition(id);
        id = tmp.substring(tmp.indexOf("C"), tmp.length()).substring(1);
        return Integer.parseInt(id);
    }

    public static int getRowIndex(String id) {
        String tmp = getCellPosition(id);
        id = tmp.substring(tmp.indexOf("R") + 1, tmp.indexOf("C"));
        return Integer.parseInt(id);
    }

    public static String getCellPosition(String id) {
        return id.substring(id.lastIndexOf("/") + 1);
    }

    public static SpreadsheetQuery createSpreadSheetQuery(String spreadSheetName,
                                                          URL spreadSheetFeedUrl) {
        SpreadsheetQuery spreadsheetQuery = new SpreadsheetQuery(spreadSheetFeedUrl);
        spreadsheetQuery.setTitleQuery(spreadSheetName);
        spreadsheetQuery.setTitleExact(true);
        return spreadsheetQuery;
    }
    
    public static WorksheetQuery createWorkSheetQuery(URL workSheetFeedUrl) {
        return new WorksheetQuery(workSheetFeedUrl);
    }

    public static ParamInfo findParam(ColumnInfo columnInfo, ParamInfo[] params) {
        ParamInfo param = null;
        for (ParamInfo tmpParam : params) {
            if (columnInfo.getName().equals(tmpParam.getName())) {
                param = tmpParam;
                break;
            }
        }
        return param;
    }

    /**
     * Method to write excel data to registry or file output streams.
     *
     * @param workbook
     * @param filePath
     * @throws SQLException
     */
    public static void writeRecords(Workbook workbook, String filePath) throws SQLException {
        OutputStream out = null;
        PipedInputStream pin = null;
        try {
            /*
                Security Comment :
                This file path is trustworthy, this path is configured in the dbs file.
            */
            if (isRegistryPath(filePath)) {
                try {
                    RegistryService registryService = SQLDriverDSComponent.getRegistryService();
                    if (registryService == null) {
                        throw new SQLException("DBUtils.getInputStreamFromPath（）：注册表服务不可用");
                    }
                    Registry registry;
                    if (filePath.startsWith(CONF_REGISTRY_PATH_PREFIX)) {
                        if (filePath.length() > CONF_REGISTRY_PATH_PREFIX.length()) {
                            filePath = filePath.substring(CONF_REGISTRY_PATH_PREFIX.length());
                            registry = registryService.getConfigSystemRegistry(getCurrentTenantId());
                        } else {
                            throw new SQLException("给定的配置注册表路径为空");
                        }
                    } else {
                        if (filePath.length() > GOV_REGISTRY_PATH_PREFIX.length()) {
                            filePath = filePath.substring(GOV_REGISTRY_PATH_PREFIX.length());
                            registry = registryService.getGovernanceSystemRegistry(getCurrentTenantId());
                        } else {
                            throw new SQLException("给定的管理注册表路径为空");
                        }
                    }
                    if (registry.resourceExists(filePath)) {
                        pin = new PipedInputStream();
                        out = new PipedOutputStream(pin);
                        new WorkBookOutputWriter(workbook,out).start();
                        Resource serviceResource = registry.get(filePath);
                        serviceResource.setContentStream(pin);
                        registry.put(filePath, serviceResource);
                    } else {
                        throw new SQLException(
                                "给定的XSLT资源路径位于: '" + filePath + "' 不存在");
                    }
                } catch (RegistryException e) {
                    throw new SQLException(e);
                }
            } else {
                File file = new File(filePath);
                if (filePath.startsWith("." + File.separator) || filePath.startsWith(".." + File.separator)) {
                /* this is a relative path */
                    filePath = file.getAbsolutePath();
                }
                out = new FileOutputStream(filePath);
                workbook.write(out);
            }
        } catch (FileNotFoundException e) {
            throw new SQLException("定位Excel数据源时出错", e);
        } catch (IOException e) {
            throw new SQLException("Error occurred while writing the records to the EXCEL " +
                                   "data source", e);
        } finally {
            if (pin != null) {
                try {
                    pin.close();
                } catch (IOException ignore) {

                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignore) {

                }
            }
        }
    }

    /**
     * Creates and returns an InputStream from the file path / http location given.
     *
     * @throws IOException, SQLException
     * @see java.io.InputStream
     */
    public static InputStream getInputStreamFromPath(String path) throws IOException, SQLException {
        InputStream ins;
        if (path.startsWith("http://")) {
            /* This is a url file path */
            /*
                Security Comment :
                This file path is trustworthy, this path is configured in the dbs file.
            */
            URL url = new URL(path);
            ins = url.openStream();
        } else if (isRegistryPath(path)) {
            try {
                RegistryService registryService = SQLDriverDSComponent.getRegistryService();
                if (registryService == null) {
                    throw new SQLException("DBUtils.getInputStreamFromPath(): 注册表服务不可用");
                }
                Registry registry;
                if (path.startsWith(CONF_REGISTRY_PATH_PREFIX)) {
                    if (path.length() > CONF_REGISTRY_PATH_PREFIX.length()) {
                        path = path.substring(CONF_REGISTRY_PATH_PREFIX.length());
                        registry = registryService.getConfigSystemRegistry(getCurrentTenantId());
                    } else {
                        throw new SQLException("给定的配置注册表路径为空");
                    }
                } else {
                    if (path.length() > GOV_REGISTRY_PATH_PREFIX.length()) {
                        path = path.substring(GOV_REGISTRY_PATH_PREFIX.length());
                        registry = registryService.getGovernanceSystemRegistry(getCurrentTenantId());
                    } else {
                        throw new SQLException("给定的管理注册表路径为空");
                    }
                }
                if (registry.resourceExists(path)) {
                    Resource serviceResource = registry.get(path);
                    ins = serviceResource.getContentStream();
                } else {
                    throw new SQLException(
                            "给定的XSLT资源路径位于: '" + path + "' 不存在");
                }
            } catch (RegistryException e) {
                throw new SQLException(e);
            }
        } else {
            File file = new File(path);
            if (path.startsWith("." + File.separator) || path.startsWith(".." + File.separator)) {
                /* this is a relative path */
                path = file.getAbsolutePath();
            }
            /* local file */
            ins = new FileInputStream(path);
        }
        return ins;
    }

    public static boolean isRegistryPath(String path) {
        if (path.startsWith(CONF_REGISTRY_PATH_PREFIX) || path.startsWith(GOV_REGISTRY_PATH_PREFIX)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns the best effort way of finding the current tenant id,
     * even if this is not in a current message request, i.e. deploying services.
     * Assumption: when tenants other than the super tenant is activated,
     * the registry service must be available. So, the service deployment and accessing the registry,
     * will happen in the same thread, without the callbacks being used.
     *
     * @return The tenant id
     */
    public static int getCurrentTenantId() {
        try {
            int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
            if (tenantId == -1) {
                throw new RuntimeException("Tenant id cannot be -1");
            }
            return tenantId;
        } catch (NoClassDefFoundError e) { // Workaround for Unit Test failure
            return MultitenantConstants.SUPER_TENANT_ID;
        } catch (ExceptionInInitializerError e) {
            return MultitenantConstants.SUPER_TENANT_ID;
        }
    }

}
