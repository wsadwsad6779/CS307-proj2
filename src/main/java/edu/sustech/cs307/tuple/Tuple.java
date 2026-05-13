package edu.sustech.cs307.tuple;

import edu.sustech.cs307.exception.DBException;
import edu.sustech.cs307.exception.ExceptionTypes;
import edu.sustech.cs307.meta.TabCol;
import edu.sustech.cs307.value.Value;
import edu.sustech.cs307.value.ValueComparer;
import edu.sustech.cs307.value.ValueType;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;
import org.pmw.tinylog.Logger;

public abstract class Tuple {
    public abstract Value getValue(TabCol tabCol) throws DBException;

    public abstract TabCol[] getTupleSchema();

    public abstract Value[] getValues() throws DBException;

    public boolean eval_expr(Expression expr) throws DBException {
        return evaluateCondition(this, expr);
    }

    private boolean evaluateCondition(Tuple tuple, Expression whereExpr) throws DBException {
        if (whereExpr instanceof AndExpression andExpr) {
            return evaluateCondition(tuple, andExpr.getLeftExpression())
                    && evaluateCondition(tuple, andExpr.getRightExpression());
        } else if (whereExpr instanceof OrExpression orExpr) {
            return evaluateCondition(tuple, orExpr.getLeftExpression())
                    || evaluateCondition(tuple, orExpr.getRightExpression());
        } else if (whereExpr instanceof BinaryExpression binaryExpression) {
            return evaluateBinaryExpression(tuple, binaryExpression);
        } else if (whereExpr instanceof InExpression inExpr) {
            return evaluateInExpression(tuple, inExpr);
        } else if (whereExpr instanceof net.sf.jsqlparser.expression.Parenthesis paren) {
            return evaluateCondition(tuple, paren.getExpression());
        } else if (whereExpr instanceof net.sf.jsqlparser.expression.NotExpression notExpr) {
            return !evaluateCondition(tuple, notExpr.getExpression());
        } else {
            return true;
        }
    }

    @SuppressWarnings("deprecation")
    private boolean evaluateInExpression(Tuple tuple, InExpression inExpr) throws DBException {
        Expression leftExpr = inExpr.getLeftExpression();
        Expression rightExpr = inExpr.getRightExpression();

        // 获取左值
        Value leftValue = null;
        if (leftExpr instanceof Column leftColumn) {
            String table_name = leftColumn.getTableName();
            if (tuple instanceof TableTuple tableTuple) table_name = tableTuple.getTableName();
            leftValue = tuple.getValue(new TabCol(table_name, leftColumn.getColumnName()));
        } else {
            leftValue = getConstantValue(leftExpr);
        }
        if (leftValue == null) return !inExpr.isNot();

        // 获取 IN 列表值
        if (rightExpr instanceof ExpressionList<?> exprList) {
            for (var item : exprList.getExpressions()) {
                Value rightValue = getConstantValue((Expression) item);
                if (rightValue != null && ValueComparer.compare(leftValue, rightValue) == 0) {
                    return !inExpr.isNot(); // IN: 找到返回true, NOT IN: 找到返回false
                }
            }
        }
        return inExpr.isNot(); // IN: 没找到返回false, NOT IN: 没找到返回true
    }

    private boolean evaluateBinaryExpression(Tuple tuple, BinaryExpression binaryExpr) {
        Expression leftExpr = binaryExpr.getLeftExpression();
        Expression rightExpr = binaryExpr.getRightExpression();
        String operator = binaryExpr.getStringExpression();
        Value leftValue = null;
        Value rightValue = null;

        try {
            if (leftExpr instanceof Column leftColumn) {
                //get table name
                String table_name = leftColumn.getTableName();
                if (tuple instanceof TableTuple) {
                    TableTuple tableTuple = (TableTuple) tuple;
                    table_name = tableTuple.getTableName();
                }
                leftValue = tuple.getValue(new TabCol(table_name, leftColumn.getColumnName()));
                if (leftValue == null) {
                    Logger.warn("Column '{}' does not exist in table '{}'", leftColumn.getColumnName(), table_name);
                    return false;
                }
                if (leftValue.type == ValueType.CHAR) {
                    leftValue = new Value(leftValue.toString());
                }
            } else {
                leftValue = getConstantValue(leftExpr); // Handle constant left value
            }

            if (rightExpr instanceof Column rightColumn) {
                //get table name
                String table_name = rightColumn.getTableName();
                if (tuple instanceof TableTuple) {
                    TableTuple tableTuple = (TableTuple) tuple;
                    table_name = tableTuple.getTableName();
                }
                rightValue = tuple.getValue(new TabCol(table_name, rightColumn.getColumnName()));
                if (rightValue == null) {
                    Logger.warn("Column '{}' does not exist in table '{}'", rightColumn.getColumnName(), table_name);
                    return false;
                }
            } else {
                rightValue = getConstantValue(rightExpr); // Handle constant right value

            }

            if (leftValue == null || rightValue == null)
                return false;

            int comparisonResult = ValueComparer.compare(leftValue, rightValue);
            if (operator.equals("=")) {
                return comparisonResult == 0;
            } else if (operator.equals(">")) {
                return comparisonResult > 0;
            } else if (operator.equals(">=")) {
                return comparisonResult >= 0;
            } else if (operator.equals("<")) {
                return comparisonResult < 0;
            } else if (operator.equals("<=")) {
                return comparisonResult <= 0;
            }

        } catch (DBException e) {
            e.printStackTrace(); // Handle exception properly
        }
        return false;
    }

    private Value getConstantValue(Expression expr) {
        if (expr instanceof StringValue) {
            return new Value(((StringValue) expr).getValue(), ValueType.CHAR);
        } else if (expr instanceof DoubleValue) {
            return new Value(((DoubleValue) expr).getValue(), ValueType.FLOAT);
        } else if (expr instanceof LongValue) {
            return new Value(((LongValue) expr).getValue(), ValueType.INTEGER);
        }
        return null; // Unsupported constant type
    }

    public Value evaluateExpression(Expression expr) throws DBException {
        if (expr instanceof StringValue) {
            return new Value(((StringValue) expr).getValue(), ValueType.CHAR);
        } else if (expr instanceof DoubleValue) {
            return new Value(((DoubleValue) expr).getValue(), ValueType.FLOAT);
        } else if (expr instanceof LongValue) {
            return new Value(((LongValue) expr).getValue(), ValueType.INTEGER);
        } else if (expr instanceof Column) {
            Column col = (Column) expr;
            return getValue(new TabCol(col.getTableName(), col.getColumnName()));
        } else {
            throw new DBException(ExceptionTypes.UnsupportedExpression(expr));
        }
    }

}
