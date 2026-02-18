package io.steviemul.sherwood.server.service.rules;

import static org.springframework.ai.model.ModelOptionsUtils.objectToMap;

import io.steviemul.sherwood.server.entity.rule.DocumentResponse;
import io.steviemul.sherwood.server.entity.rule.QueryAdjustments;
import io.steviemul.sherwood.server.entity.rule.RuleRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder.Op;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
public class RulesVectorService {

  private final VectorStore rulesVectorStore;
  private final TemplateService templateService;

  private static final String VENDOR = "vendor";

  public void save(RuleRequest ruleRequest) {

    Document document = ruleRequestToDocument(ruleRequest);

    rulesVectorStore.add(List.of(document));
  }

  public void save(List<RuleRequest> ruleRequests) {

    List<Document> documents = ruleRequests.stream().map(this::ruleRequestToDocument).toList();

    rulesVectorStore.add(documents);
  }

  public Document ruleRequestToDocument(RuleRequest ruleRequest) {

    String embeddingContents = templateService.renderRuleEmbedding(ruleRequest);

    return new Document(embeddingContents, templateService.objectToMap(ruleRequest));
  }

  public Document getByRuleId(String ruleId) {

    FilterExpressionBuilder builder = new FilterExpressionBuilder();

    Filter.Expression expression = builder.eq("id", ruleId).build();

    SearchRequest.Builder searchRequestBuilder =
        SearchRequest.builder().query("").filterExpression(expression).topK(1);

    return rulesVectorStore.similaritySearch(searchRequestBuilder.build()).getFirst();
  }

  public List<DocumentResponse> search(
      String vendor, QueryAdjustments adjustments, RuleRequest ruleRequest) {
    return similaritySearch(vendor, adjustments, ruleRequest);
  }

  public List<DocumentResponse> similaritySearch(
      String vendor, QueryAdjustments adjustments, RuleRequest ruleRequest) {

    String embeddingContents = templateService.renderRuleEmbedding(ruleRequest);

    SearchRequest.Builder searchRequestBuilder =
        SearchRequest.builder()
            .query(embeddingContents)
            .similarityThreshold(adjustments.threshold())
            .topK(adjustments.size());

    createFilterExpression(vendor, adjustments.exact(), ruleRequest)
        .ifPresent(searchRequestBuilder::filterExpression);

    SearchRequest searchRequest = searchRequestBuilder.build();

    return rulesVectorStore.similaritySearch(searchRequest).stream()
        .map(this::documentToResponse)
        .toList();
  }

  private DocumentResponse documentToResponse(Document document) {
    return new DocumentResponse(document.getScore(), document.getMetadata());
  }

  private Optional<Filter.Expression> createFilterExpression(
      String vendor, boolean exact, RuleRequest ruleRequest) {

    FilterExpressionBuilder builder = new FilterExpressionBuilder();

    Op op = null;

    if (StringUtils.hasText(vendor)) {
      op = builder.eq(VENDOR, vendor);
    }

    if (exact) {
      Op metatadaOp = createMetadataFilterOp(ruleRequest);

      if (metatadaOp != null) {
        op = (op == null) ? metatadaOp : builder.and(op, metatadaOp);
      }
    }

    return Optional.ofNullable(op).map(Op::build);
  }

  private Op createMetadataFilterOp(RuleRequest request) {

    FilterExpressionBuilder builder = new FilterExpressionBuilder();

    Op op = null;
    Map<String, Object> data = objectToMap(request);

    for (Map.Entry<String, Object> entry : data.entrySet()) {
      if (entry.getValue() != null) {
        Op eq = builder.eq(entry.getKey(), entry.getValue());

        op = (op == null) ? eq : builder.and(op, eq);
      }
    }

    return op;
  }
}
