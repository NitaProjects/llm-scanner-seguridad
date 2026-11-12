package com.llmscannerseguridad.integration;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import com.sun.net.httpserver.HttpServer;
import com.llmscannerseguridad.application.target.*;
import com.llmscannerseguridad.application.project.*;
import com.llmscannerseguridad.application.scan.*;
import com.llmscannerseguridad.application.execution.*;
import com.llmscannerseguridad.application.evaluation.*;
import com.llmscannerseguridad.adapters.out.evaluation.*;
import com.llmscannerseguridad.adapters.in.rest.target.*;
import com.llmscannerseguridad.domain.target.*;
import com.llmscannerseguridad.domain.evaluation.*;
import com.llmscannerseguridad.config.ScanWorkerEntry;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="SCAN_TEST_DB_URL",matches="jdbc:postgresql://localhost:5432/llm_scanner_seguridad_test")
@Timeout(90)
class OpenAiCompatiblePostgresIntegrationTest {
    @TempDir Path directory;
    static final String KEY=Base64.getEncoder().encodeToString(new byte[32]);
    static final String CHAT="{\"adapterType\":\"OPENAI_COMPATIBLE\",\"endpoint\":\"https://chat.example.invalid/completions\",\"model\":\"fixture-model\",\"systemPrompt\":\"synthetic system\",\"authType\":\"BEARER_TOKEN\",\"authConfig\":{\"secretValue\":\"synthetic-token\"}}";
    static final String REST="{\"url\":\"https://rest.example.invalid/chat\",\"requestTemplate\":\"{\\\"m\\\":\\\"{{PROMPT}}\\\"}\",\"responsePath\":\"$.text\",\"authType\":\"NONE\"}";

    @Test void savesChatWithBearerAndNullOptionalFieldsThroughPostAndPut() throws Exception {
        try (var db = new ScanTestDatabase()) {
            var app = db.openSecurity(Map.of("TARGET_CREDENTIAL_ENCRYPTION_KEY", KEY, "logging.level.root", "ERROR"));
            var project = UUID.randomUUID();
            db.jdbc().update("insert into projects(id,name,created_at,updated_at) values(?,'fixture',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)", project);
            var mvc = MockMvcBuilders.standaloneSetup(app.getBean(TargetController.class))
                    .setControllerAdvice(new TargetExceptionHandler()).build();
            String route = "/api/projects/" + project + "/target";
            String input = """
                    {"adapterType":"OPENAI_COMPATIBLE","endpoint":"https://api.groq.com/openai/v1/chat/completions",
                     "model":"qwen/qwen3.8-27b","name":null,"systemPrompt":null,"timeoutMs":20000,
                     "authType":"BEARER_TOKEN","authConfig":{"secretValue":"synthetic-token"}}
                    """;
            mvc.perform(post(route).contentType("application/json").content(input)).andExpect(status().isCreated());
            mvc.perform(put(route).contentType("application/json").content(input)).andExpect(status().isOk());
            mvc.perform(get(route)).andExpect(status().isOk()).andExpect(jsonPath("$.hasCredentials").value(true));
            var row = db.jdbc().queryForMap("""
                    select adapter_type, model, name, system_prompt, request_template, response_path, auth_type,
                           timeout_ms, credential_ciphertext is not null as has_credential
                    from target_configurations where project_id=?
                    """, project);
            assertThat(row).containsEntry("adapter_type", "OPENAI_COMPATIBLE")
                    .containsEntry("model", "qwen/qwen3.8-27b").containsEntry("auth_type", "BEARER_TOKEN")
                    .containsEntry("timeout_ms", 20000).containsEntry("has_credential", true)
                    .containsEntry("name", null).containsEntry("system_prompt", null)
                    .containsEntry("request_template", null).containsEntry("response_path", null);
        }
    }

    @Test void migrationPreservesLegacyAndChatColumnsAndRejectsUnknownNullBranches() throws Exception {
        try(var db=new ScanTestDatabase()) {
            db.migrate("7");
            var project=UUID.randomUUID();var target=UUID.randomUUID();
            db.jdbc().update("insert into projects(id,name,created_at,updated_at) values(?,'fixture',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",project);
            db.jdbc().update("insert into target_configurations(id,project_id,url,request_template,response_path,auth_type,timeout_ms,created_at,updated_at) values(?,?,'https://old.invalid','{\"m\":\"{{PROMPT}}\"}','$.text','NONE',1000,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",target,project);
            var before=db.jdbc().queryForMap("select * from target_configurations where id=?",target);
            db.migrate(null);
            var after=db.jdbc().queryForMap("select * from target_configurations where id=?",target);
            before.forEach((key,value)->assertThat(after.get(key)).isEqualTo(value));
            assertThat(after.get("adapter_type")).isEqualTo("REST_LEGACY");assertThat(after.get("model")).isNull();
            assertThat(after.get("system_prompt")).isNull();
            for(String mutation:List.of("model='wrong'","system_prompt='wrong'","adapter_type=NULL","adapter_type='UNKNOWN'","request_template=NULL","response_path=NULL",
                    "adapter_type='OPENAI_COMPATIBLE',model=NULL,request_template=NULL,response_path=NULL",
                    "adapter_type='OPENAI_COMPATIBLE',model='fixture-model',request_template=NULL,response_path=NULL")) {
                assertThatThrownBy(()->db.jdbc().update("update target_configurations set "+mutation+" where id=?",target)).isInstanceOf(RuntimeException.class);
            }
            var c=db.openSecurity(Map.of("TARGET_CREDENTIAL_ENCRYPTION_KEY",KEY,"logging.level.root","ERROR"));
            var mapper=new TargetRequestMapper();c.getBean(UpdateTargetConfigurationUseCase.class).execute(project,mapper.command(CHAT));
            var saved=db.jdbc().queryForMap("select * from target_configurations where id=?",target);
            assertThat(saved.get("request_template")).isNull();assertThat(saved.get("response_path")).isNull();
            assertThat(saved.get("model")).isEqualTo("fixture-model");assertThat(saved.get("system_prompt")).isEqualTo("synthetic system");
            for(String mutation:List.of("model=NULL","model=' '","request_template='{}'","response_path='$.text'",
                    "auth_type='NONE',credential_ciphertext=NULL","system_prompt=' '","system_prompt=repeat('x',16385)")) {
                assertThatThrownBy(()->db.jdbc().update("update target_configurations set "+mutation+" where id=?",target)).isInstanceOf(RuntimeException.class);
            }
            db.jdbc().update("update target_configurations set system_prompt=NULL where id=?",target);
            assertThat(db.jdbc().queryForMap("select * from target_configurations where id=?",target).get("system_prompt")).isNull();
            db.jdbc().update("update target_configurations set system_prompt='synthetic system' where id=?",target);
            db.closeApplication(c);c=db.openSecurity(Map.of("TARGET_CREDENTIAL_ENCRYPTION_KEY",KEY,"logging.level.root","ERROR"));
            var read=c.getBean(GetTargetConfigurationUseCase.class).execute(project).settings();
            assertThat(read.adapterType()).isEqualTo(TargetAdapterType.OPENAI_COMPATIBLE);assertThat(read.model()).isEqualTo("fixture-model");
            assertThat(read.systemPrompt()).isEqualTo("synthetic system");
        }
    }

    @Test void legacyPutLosesConcurrentConversionWith409AndNoPartialWrite() throws Exception {
        try(var db=new ScanTestDatabase();var executor=Executors.newSingleThreadExecutor()) {
            var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var paused=new ThreadLocal<Boolean>();
            var c=db.openSecurity(Map.of("TARGET_CREDENTIAL_ENCRYPTION_KEY",KEY,"logging.level.root","ERROR"),app->
                app.registerBean("raceRepository",TargetConfigurationRepositoryPort.class,()->{
                    var delegate=app.getBean("targetConfigurationRepositoryPort",TargetConfigurationRepositoryPort.class);
                    return new TargetConfigurationRepositoryPort(){
                        public boolean existsByProjectId(UUID id){return delegate.existsByProjectId(id);}
                        public Optional<TargetConfiguration> findByProjectId(UUID id){return delegate.findByProjectId(id);}
                        public void insert(TargetConfiguration t){delegate.insert(t);}
                        public boolean replace(TargetConfiguration t,TargetAdapterType expected){
                            if(Boolean.TRUE.equals(paused.get())){entered.countDown();await(release);}
                            return delegate.replace(t,expected);
                        }
                    };
                },bean->bean.setPrimary(true)));
            var project=c.getBean(CreateProjectUseCase.class).execute(new CreateProjectCommand("race",null)).id();
            var mapper=new TargetRequestMapper();c.getBean(ConfigureTargetUseCase.class).execute(project,mapper.command(REST));
            var mvc=MockMvcBuilders.standaloneSetup(c.getBean(TargetController.class)).setControllerAdvice(new TargetExceptionHandler()).build();
            var pending=executor.submit(()->{paused.set(true);try{return mvc.perform(put("/api/projects/"+project+"/target").contentType("application/json").content(REST)).andReturn().getResponse();}finally{paused.remove();}});
            try {
                assertThat(entered.await(10,TimeUnit.SECONDS)).isTrue();
                mvc.perform(put("/api/projects/"+project+"/target").contentType("application/json").content(CHAT)).andExpect(status().isOk());
                var committed=db.jdbc().queryForMap("select * from target_configurations where project_id=?",project);
                release.countDown();var rejected=pending.get(10,TimeUnit.SECONDS);
                assertThat(rejected.getStatus()).isEqualTo(409);assertThat(rejected.getContentAsString()).contains("TARGET_TYPE_REQUIRED");
                var unchanged=db.jdbc().queryForMap("select * from target_configurations where project_id=?",project);
                assertThat((byte[])unchanged.remove("credential_ciphertext")).containsExactly((byte[])committed.remove("credential_ciphertext"));
                assertThat(unchanged).isEqualTo(committed);
                mvc.perform(put("/api/projects/"+project+"/target").contentType("application/json").content(REST)).andExpect(status().isConflict());
                var explicitRest=REST.replaceFirst("\\{","{\"adapterType\":\"REST_LEGACY\",");
                mvc.perform(put("/api/projects/"+project+"/target").contentType("application/json").content(explicitRest)).andExpect(status().isOk());
                mvc.perform(put("/api/projects/"+project+"/target").contentType("application/json").content(REST)).andExpect(status().isOk());
                mvc.perform(put("/api/projects/"+project+"/target").contentType("application/json").content(CHAT)).andExpect(status().isOk());
                assertThat(db.jdbc().queryForObject("select adapter_type from target_configurations where project_id=?",String.class,project)).isEqualTo("OPENAI_COMPATIBLE");
            } finally {release.countDown();}
        }
    }

    @Test void chatPipelinePreservesFourEvaluationsFindingsScoreAndReportWithSyntheticFakes() throws Exception {
        for(boolean contextAvailable:List.of(false,true)) {
            var calls=new AtomicInteger();var evaluatorCalls=new AtomicInteger();
            var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
            var json=tools.jackson.databind.json.JsonMapper.builder().build();
            server.createContext("/",exchange->{
                calls.incrementAndGet();String request=new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);
                String category=request.contains("SCANNER_OVERRIDE_DEMO")?"LLM01":request.contains("SCANNER_PRIVATE_CANARY")?"LLM02":request.contains("CONTEXT_NOT_DISCLOSED")?"LLM08":"LLM10";
                String text=contextAvailable?EvaluationFixtures.vulnerable(category):"SAFE";
                var body=json.writeValueAsBytes(Map.of("choices",List.of(Map.of("message",Map.of("role","assistant","content",text),"finish_reason","stop"))));
                exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(200,body.length);exchange.getResponseBody().write(body);exchange.close();
            });server.start();
            try(var db=new ScanTestDatabase()) {
                String origin="http://127.0.0.1:"+server.getAddress().getPort();Path file=directory.resolve("scenario-"+contextAvailable+".json");
                var c=db.openSecurity(Map.of("TARGET_CREDENTIAL_ENCRYPTION_KEY",KEY,"logging.level.root","ERROR","TARGET_LOCAL_DEMO_ENABLED",true,
                    "TARGET_LOCAL_DEMO_ORIGINS",origin,"EVALUATOR_EXPORT_ENABLED",contextAvailable,"EVALUATOR_ALLOWED_SCENARIOS","chat-fixture",
                    "EVALUATION_SCENARIO_FILE",contextAvailable?file.toString():""),app->app.registerBean("chatEvaluator",EvaluatorPort.class,
                    ()->(input,budget)->{evaluatorCalls.incrementAndGet();return new EvaluatorOutcome(EvaluationFixtures.assessment(input,Verdict.VULNERABLE),null,"fixture-model",1L,1L,1L,null,"fixture");},bean->bean.setPrimary(true)));
                UUID project=c.getBean(CreateProjectUseCase.class).execute(new CreateProjectCommand("chat",null)).id();
                c.getBean(ConfigureTargetUseCase.class).execute(project,new TargetRequestMapper().command(CHAT.replace("https://chat.example.invalid/completions",origin+"/chat")));
                var target=c.getBean(TargetConfigurationRepositoryPort.class).findByProjectId(project).orElseThrow();
                if(contextAvailable){var entries=c.getBean(SecurityTestCatalogPort.class).findAllOrdered().stream().map(t->new EvaluationScenarioPort.Entry(t.code(),1,EvaluationFixtures.context(t.category().name()),List.of(EvaluationFixtures.vulnerable(t.category().name())))).toList();
                    Files.writeString(file,EvaluationJson.write(new LocalEvaluationScenarioAdapter.Manifest(1,List.of(new LocalEvaluationScenarioAdapter.ManifestScenario("chat-fixture","1",project,target.id(),target.updatedAt().toString(),entries,new LocalEvaluationScenarioAdapter.ExportPolicy(true,"OPENAI"))))));}
                var scan=c.getBean(ScanLifecyclePort.class).create(project);c.getBean(ScanWorkerEntry.class).execute(scan.id());
                assertThat(c.getBean(GetScanUseCase.class).execute(project,scan.id()).status()).isEqualTo(com.llmscannerseguridad.domain.scan.ScanStatus.COMPLETED);
                assertThat(calls).hasValue(4);assertThat(evaluatorCalls).hasValue(contextAvailable?4:0);
                assertThat(c.getBean(TestExecutionRepositoryPort.class).findByScanId(scan.id())).hasSize(4);
                var evaluations=c.getBean(EvaluationRepositoryPort.class);assertThat(evaluations.listEvaluations(scan.id()).items()).hasSize(4).allSatisfy(e->assertThat(e.finalVerdict()).isEqualTo(contextAvailable?Verdict.VULNERABLE:Verdict.INCONCLUSIVE));
                assertThat(evaluations.listFindings(scan.id())).hasSize(contextAvailable?4:0);
                assertThat(c.getBean(com.llmscannerseguridad.application.scoring.GetScanScoreUseCase.class).execute(project,scan.id()).score().globalScore()).isEqualTo(contextAvailable?55:0);
                assertThat(c.getBean(com.llmscannerseguridad.application.reporting.GetSecurityReportUseCase.class).execute(project,scan.id()).report().reportVersion()).isEqualTo("report-v1");
            } finally {server.stop(0);}
        }
    }
    static void await(CountDownLatch latch){try{if(!latch.await(15,TimeUnit.SECONDS))throw new IllegalStateException("fixture timeout");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("fixture interrupted");}}
}
