package mg.school.hei.endpoint.event.consumer.model;

import mg.school.hei.PojaGenerated;
import mg.school.hei.endpoint.event.model.PojaEvent;

@PojaGenerated
public record TypedEvent(String typeName, PojaEvent payload) {}
