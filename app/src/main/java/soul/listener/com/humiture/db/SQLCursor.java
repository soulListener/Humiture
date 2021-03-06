package soul.listener.com.humiture.db;

import com.mysql.jdbc.Statement;
import com.orhanobut.logger.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import soul.listener.com.humiture.base.BaseActivity;
import soul.listener.com.humiture.model.BlocksModel;
import soul.listener.com.humiture.model.LogModel;
import soul.listener.com.humiture.model.PartDataSelectionModel;
import soul.listener.com.humiture.model.ResidentModel;
import soul.listener.com.humiture.model.SqlFactory;
import soul.listener.com.humiture.model.SqlInfoCallBack;
import soul.listener.com.humiture.model.SystemUserModel;
import soul.listener.com.humiture.model.TemperatureModel;
import soul.listener.com.humiture.util.Constants;
import soul.listener.com.humiture.util.SqlStateCode;
import soul.listener.com.humiture.util.StringUtils;

import static soul.listener.com.humiture.util.MyApplication.connection;

/**
 * @author kuan
 *         Created 2017/10/24.
 * @description 数据库操作类
 */

public class SQLCursor {
    //禁止外部实例化
    private SQLCursor() {
    }

    /**
     * 获取连接对象
     *
     * @return
     * @throws SQLException
     */
    public static Connection getInstance() throws SQLException {
        if (connection == null) {
            synchronized (SQLCursor.class) {
                if (connection == null) {
                    connection = DriverManager.getConnection(Constants.SQLITEURL, Constants.SQLITEUSER, Constants.SQLITEPW);
                }
            }
        }
        //如果数据库连接关闭则重新连接
        if (connection.isClosed()){
        connection = DriverManager.getConnection(Constants.SQLITEURL, Constants.SQLITEUSER, Constants.SQLITEPW);
        }
        return connection;
    }

    /**
     * 返回全部数据
     *
     * @param tableNameNo
     * @return resultSet
     * @throws SQLException
     */
    public static void getData(final BaseActivity mView,final int tableNameNo, final SqlInfoCallBack sqlInfoCallBack) throws SQLException {
        mView.showDialog();
        final ArrayList sqlFactoryList = new ArrayList();
        final Statement[] statement = {null};
        Flowable.just(tableNameNo)
                .subscribeOn(Schedulers.newThread())
                //在新线程中变换数据并发送
                .map(new Function<Integer, ResultSet>() {
                    @Override
                    public ResultSet apply(Integer s) throws Exception {
                        statement[0] = (Statement) getInstance().createStatement();
                        String sqlMessage = "Select * From " + StringUtils.dealTableName(s);
                        Logger.e("getData   sqlMessage=========="+sqlMessage);
                        return statement[0].executeQuery(sqlMessage);
                    }
                })
                //返回主线程
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<ResultSet>() {
                    @Override
                    public void accept(ResultSet resultSet) throws Exception {
                        fillContent(sqlFactoryList, tableNameNo, resultSet, statement[0]);

                        mView.hideDialog();

                        if (sqlFactoryList.size()>0) {
                            sqlInfoCallBack.Success(sqlFactoryList);
                        }else{
                            sqlInfoCallBack.Faild(SqlStateCode.STSTE_NODATA);
                        }
                    }
                });
    }

    /**
     * 返回部分数据
     *
     * @param tableNameNo
     * @param parts
     * @param startlimit
     * @param endlimit
     * @return resultSet
     * @throws SQLException
     */
    public static void getPartData(final int tableNameNo, final String[] parts, final int startlimit, final int endlimit,final SqlInfoCallBack sqlInfoCallBack) throws SQLException {
        final ArrayList sqlFactoryList = new ArrayList();
        final Statement[] statement = {null};
        Flowable.just(tableNameNo)
                .subscribeOn(Schedulers.newThread())
                //在新线程中变换数据并发送
                .map(new Function<Integer, ResultSet>() {
                    @Override
                    public ResultSet apply(Integer s) throws Exception {
                        statement[0] = (Statement) getInstance().createStatement();
                        StringBuilder stringBuilder = new StringBuilder();
                        for (int i = 0; i < parts.length; i++) {
                            if (i != parts.length) {
                                stringBuilder.append(parts + ",");
                            } else {
                                stringBuilder.append(parts);
                            }
                        }

                        String tableName = StringUtils.dealTableName(s);
                        String sqlMessage = "Select " + stringBuilder.toString() + " From " + tableName + " limit " + startlimit + " ," + endlimit;
                        Logger.e("getPartData      sqlMessage=========="+sqlMessage);
                        return statement[0].executeQuery(sqlMessage);
                    }
                })
                //返回主线程
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<ResultSet>() {
                    @Override
                    public void accept(ResultSet resultSet) throws Exception {
                        fillContent(sqlFactoryList, tableNameNo, resultSet, statement[0]);
                        if (sqlFactoryList.size()>0){
                            sqlInfoCallBack.Success(sqlFactoryList);
                        }else{
                            sqlInfoCallBack.Faild(SqlStateCode.STSTE_NODATA);
                        }
                    }
                });
    }

    /**
     * 根据条件查询部分数据
     *
     * @param tableNameNo 数据库表名
     * @param parts       要展示的数据
     * @param selections  条件
     * @param hazyOrExact 精确还是模糊查询
     * @param conditions  条件
     * @param startlimit  开始限制
     * @param endlimit    结尾限制
     * @return
     * @throws SQLException
     */
    public static void getPartDataBySelection(final int tableNameNo, final String[] parts, final String[] selections, final String[] hazyOrExact, final String[] conditions, final int startlimit, final int endlimit,final SqlInfoCallBack sqlInfoCallBack) throws SQLException {
        final ArrayList sqlFactoryList = new ArrayList();
        final Statement[] statement = {null};
        Flowable.just(tableNameNo)
                .subscribeOn(Schedulers.newThread())
                //在新线程中变换数据并发送
                .map(new Function<Integer, ResultSet>() {
                    @Override
                    public ResultSet apply(Integer s) throws Exception {
                        statement[0] = (Statement) getInstance().createStatement();

                        //拼装SQL查询语句
                        StringBuilder stringBuilder = new StringBuilder();
                        for (int i = 0; i < parts.length; i++) {
                            if (i == 0) {
                                stringBuilder.append(parts[i]);
                            } else {
                                stringBuilder.append("," + parts[i]);
                            }
                        }

                        String tableName = StringUtils.dealTableName(s);
                        String part = stringBuilder.toString();
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Select " + part + " From " + tableName + " Where ");

                        for (int i = 0; i < selections.length; i++) {
                            if (i == 0) {
                                stringBuilder.append(selections[i] + " " + hazyOrExact[i] + " '" + conditions[i] + "'");
                            } else {
                                stringBuilder.append("and " + selections[i] + " " + hazyOrExact[i] + " '" + conditions[i] + "'");
                            }
                        }

                        stringBuilder.append(" limit " + startlimit + " ," + endlimit);
                        String sqlMessage = stringBuilder.toString();
                        Logger.e("getPartDataBySelection  sqlMessage========" + sqlMessage);
                        return statement[0].executeQuery(sqlMessage);
                    }
                })
                //返回主线程
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<ResultSet>() {
                    @Override
                    public void accept(ResultSet resultSet) throws Exception {
                        fillContent(sqlFactoryList, tableNameNo, resultSet, statement[0]);
                        if (sqlFactoryList.size()>0){
                            sqlInfoCallBack.Success(sqlFactoryList);
                        }else{
                            sqlInfoCallBack.Faild(SqlStateCode.STSTE_NODATA);
                        }
                    }
                });
    }


    /**
     * 根据条件查询部分数据
     * @param model
     * @param sqlInfoCallBack
     * @throws SQLException
     */
    public static void getPartDataBySelection(final BaseActivity mView, final PartDataSelectionModel model, final SqlInfoCallBack sqlInfoCallBack) throws SQLException {
        mView.showDialog();
        final ArrayList sqlFactoryList = new ArrayList();
        final Statement[] statement = {null};
        Flowable.just(model.getTableNameNo())
                .subscribeOn(Schedulers.newThread())
                //在新线程中变换数据并发送
                .map(new Function<Integer, ResultSet>() {
                    @Override
                    public ResultSet apply(Integer s) throws Exception {
                        statement[0] = (Statement) getInstance().createStatement();
                        //拼装SQL查询语句
                        StringBuilder stringBuilder = new StringBuilder();
                        for (int i = 0; i < model.getParts().length; i++) {
                            if (i == 0) {
                                stringBuilder.append(model.getParts()[i]);
                            } else {
                                stringBuilder.append("," + model.getParts()[i]);
                            }
                        }

                        String tableName = StringUtils.dealTableName(s);
                        String part = stringBuilder.toString();
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Select " + part + " From " + tableName + " Where ");

                        for (int i = 0; i < model.getSelections().length; i++) {
                            if (i == 0) {
                                stringBuilder.append(model.getSelections()[i] + " " + model.getHazyOrExact()[i] + " '" + model.getConditions()[i] + "'");
                            } else {
                                stringBuilder.append("and " + model.getSelections()[i] + " " + model.getHazyOrExact()[i] + " '" + model.getConditions()[i] + "'");
                            }
                        }

                        stringBuilder.append(" limit " + model.getStartLimit() + " ," + model.getEndLimit());
                        String sqlMessage = stringBuilder.toString();
                        Logger.e("getPartDataBySelection  sqlMessage========" + sqlMessage);
                        return statement[0].executeQuery(sqlMessage);
                    }
                })
                //返回主线程
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<ResultSet>() {
                    @Override
                    public void accept(ResultSet resultSet) throws Exception {
                        fillContent(sqlFactoryList, model.getTableNameNo(), resultSet, statement[0]);
                        mView.hideDialog();
                        if (sqlFactoryList.size()>0){
                            sqlInfoCallBack.Success(sqlFactoryList);
                        }else{
                            sqlInfoCallBack.Faild(SqlStateCode.STSTE_NODATA);
                        }
                    }
                });
    }

    /**
     * 组装数据
     *
     * @param sqlFactoryList
     * @param tableNameNo
     * @param resultSet
     * @return
     * @throws SQLException
     */
    public static ArrayList<SqlFactory> fillContent(ArrayList<SqlFactory> sqlFactoryList, int tableNameNo, ResultSet resultSet, Statement statement) throws SQLException {
        while (resultSet.next()) {
            switch (tableNameNo) {
                case Constants.SYSUSER_TABLE_NO:
                    SystemUserModel systemUserModel = (SystemUserModel) SqlFactory.creatSqlModel(tableNameNo);
                    systemUserModel.setId(resultSet.getString(Constants.SYSUSER_ID));
                    systemUserModel.setUserId(resultSet.getString(Constants.SYSUSER_USERID));
                    systemUserModel.setUserName(resultSet.getString(Constants.SYSUSER_USERNAME));
                    systemUserModel.setUserPassword(resultSet.getString(Constants.SYSUSER_PASSWORD));
                    systemUserModel.setUserpPrivaileges(resultSet.getString(Constants.SYSUSER_USERPRIVILEGES));
                    systemUserModel.setUserType(resultSet.getString(Constants.SYSUSER_USERTYPE));
                    systemUserModel.setUserMail(resultSet.getString(Constants.SYSUSER_USERMAIL));
                    systemUserModel.setUserTel(resultSet.getString(Constants.SYSUSER_USERTEL));
                    sqlFactoryList.add(systemUserModel);
                    break;
                case Constants.BLOCKS_TABLE_NO:
                    BlocksModel bolckModel = (BlocksModel) SqlFactory.creatSqlModel(tableNameNo);
                    bolckModel.setBlocksId(resultSet.getString(Constants.BLOCK_ID));
                    bolckModel.setBlocksName(Constants.BLOCK_NAME);
                    bolckModel.setBlocksLocation(Constants.BLOCK_LOACTION);
                    bolckModel.setBlocksContact(Constants.BLOCK_CONTACT);
                    bolckModel.setBlocksContaceTel(Constants.BLOCK_CONTACETEL);
                    sqlFactoryList.add(bolckModel);
                    break;
                case Constants.RESIDENT_TABLE_NO:
                    ResidentModel residentModel = (ResidentModel) SqlFactory.creatSqlModel(tableNameNo);
                    sqlFactoryList.add(residentModel);
                    break;
                case Constants.LOG_TABLE_NO:
                    LogModel logModel = (LogModel) SqlFactory.creatSqlModel(tableNameNo);
                    sqlFactoryList.add(logModel);
                    break;
                case Constants.TEMPERATURE_TABLE_NO:
                    TemperatureModel temperatureModel = (TemperatureModel) SqlFactory.creatSqlModel(tableNameNo);
                    sqlFactoryList.add(temperatureModel);
                    break;
                default:
                    break;
            }
        }
        //关闭数据连接
        resultSet.close();
        statement.close();
        return sqlFactoryList;
    }


}
