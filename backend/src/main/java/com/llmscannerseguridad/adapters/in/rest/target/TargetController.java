package com.llmscannerseguridad.adapters.in.rest.target;

import java.net.URI;
import com.llmscannerseguridad.application.target.ConfigureTargetUseCase;
import com.llmscannerseguridad.application.target.GetTargetConfigurationUseCase;
import com.llmscannerseguridad.application.target.UpdateTargetConfigurationUseCase;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/projects/{projectId}/target", produces = MediaType.APPLICATION_JSON_VALUE)
public class TargetController {
    private final ConfigureTargetUseCase configure;
    private final GetTargetConfigurationUseCase get;
    private final UpdateTargetConfigurationUseCase update;
    private final TargetRequestMapper requests = new TargetRequestMapper();

    public TargetController(ConfigureTargetUseCase configure, GetTargetConfigurationUseCase get,
            UpdateTargetConfigurationUseCase update) {
        this.configure = configure;
        this.get = get;
        this.update = update;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> configure(@PathVariable("projectId") String projectId,
            @RequestBody String body) {
        var id = requests.identifier(projectId);
        var view = configure.execute(id, requests.command(body));
        return ResponseEntity.created(URI.create("/api/projects/" + id + "/target"))
                .body(TargetResponse.resourceFrom(view));
    }

    @GetMapping
    public Object get(@PathVariable("projectId") String projectId) {
        return TargetResponse.resourceFrom(get.execute(requests.identifier(projectId)));
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Object update(@PathVariable("projectId") String projectId, @RequestBody String body) {
        var id = requests.identifier(projectId);
        return TargetResponse.resourceFrom(update.execute(id, requests.command(body)));
    }
}
