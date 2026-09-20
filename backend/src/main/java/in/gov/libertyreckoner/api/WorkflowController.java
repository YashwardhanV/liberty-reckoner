package in.gov.libertyreckoner.api;

import in.gov.libertyreckoner.api.ApiDtos.*;
import in.gov.libertyreckoner.service.WorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController @RequestMapping("/api/workflows") @RequiredArgsConstructor
public class WorkflowController {
    private final WorkflowService workflowService;

    @GetMapping("/queue")
    public List<TaskView> queue() {
        return workflowService.queue();
    }

    @PostMapping("/{id}/start")
    public TaskView start(@PathVariable UUID id) {
        return workflowService.start(id);
    }

    @PostMapping("/{id}/complete")
    public TaskView complete(@PathVariable UUID id, @Valid @RequestBody CompleteTaskRequest request) {
        return workflowService.complete(id, request);
    }
}

