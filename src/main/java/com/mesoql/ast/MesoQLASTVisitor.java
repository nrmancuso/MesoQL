package com.mesoql.ast;

import com.mesoql.parser.MesoQLBaseVisitor;
import com.mesoql.parser.MesoQLParser;

import java.util.List;

public class MesoQLASTVisitor extends MesoQLBaseVisitor<QueryAST.Node> {

    @Override
    public QueryAST.Node visitQuery(MesoQLParser.QueryContext ctx) {
        QueryAST.SearchClause search = (QueryAST.SearchClause) visit(ctx.searchClause());
        QueryAST.WhereClause where = (QueryAST.WhereClause) visit(ctx.whereClause());
        List<QueryAST.OutputClause> outputs = ctx.outputClause().stream()
            .map(c -> (QueryAST.OutputClause) visit(c))
            .toList();
        return new QueryAST.Query(search, where, outputs);
    }

    @Override
    public QueryAST.Node visitSearchClause(MesoQLParser.SearchClauseContext ctx) {
        String source = ctx.source().getText().toLowerCase();
        return new QueryAST.SearchClause(source);
    }

    @Override
    public QueryAST.Node visitWhereClause(MesoQLParser.WhereClauseContext ctx) {
        QueryAST.SemanticClause semantic = (QueryAST.SemanticClause) visit(ctx.semanticClause());
        List<QueryAST.Filter> filters = ctx.filterClause().stream()
            .map(fc -> (QueryAST.Filter) visit(fc.filter()))
            .toList();
        return new QueryAST.WhereClause(semantic, filters);
    }

    @Override
    public QueryAST.Node visitSemanticClause(MesoQLParser.SemanticClauseContext ctx) {
        String text = stripQuotes(ctx.STRING().getText());
        return new QueryAST.SemanticClause(text);
    }

    @Override
    public QueryAST.Node visitInFilter(MesoQLParser.InFilterContext ctx) {
        String field = ctx.field().getText();
        List<String> values = ctx.stringList().STRING().stream()
            .map(t -> stripQuotes(t.getText()))
            .toList();
        return new QueryAST.InFilter(field, values);
    }

    @Override
    public QueryAST.Node visitBetweenFilter(MesoQLParser.BetweenFilterContext ctx) {
        String field = ctx.field().getText();
        double low = Double.parseDouble(ctx.numericLiteral(0).getText());
        double high = Double.parseDouble(ctx.numericLiteral(1).getText());
        return new QueryAST.BetweenFilter(field, low, high);
    }

    @Override
    public QueryAST.Node visitComparisonFilter(MesoQLParser.ComparisonFilterContext ctx) {
        String field = ctx.field().getText();
        String op = ctx.comparisonOp().getText();
        String value = ctx.literal().getText();
        if (value.startsWith("\"")) {
            value = stripQuotes(value);
        }
        return new QueryAST.ComparisonFilter(field, op, value);
    }

    @Override
    public QueryAST.Node visitSynthesizeClause(MesoQLParser.SynthesizeClauseContext ctx) {
        return new QueryAST.SynthesizeClause(stripQuotes(ctx.STRING().getText()));
    }

    @Override
    public QueryAST.Node visitClusterClause(MesoQLParser.ClusterClauseContext ctx) {
        return new QueryAST.ClusterClause();
    }

    @Override
    public QueryAST.Node visitExplainClause(MesoQLParser.ExplainClauseContext ctx) {
        return new QueryAST.ExplainClause();
    }

    @Override
    public QueryAST.Node visitLimitClause(MesoQLParser.LimitClauseContext ctx) {
        return new QueryAST.LimitClause(Integer.parseInt(ctx.INTEGER().getText()));
    }

    private String stripQuotes(String s) {
        return s.substring(1, s.length() - 1);
    }
}
