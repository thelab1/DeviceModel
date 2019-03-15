package rrb.infra.rapidsocket;

import rrb.infra.devicemodel.DeviceModelProto.MessageBlock;

/**
 *
 * @author pobzeb
 */
public interface RapidClientHandler {
    public void onOpen();

    public void onClose();

    public void onError(Exception ex);

    public MessageBlock onMessage(MessageBlock message);
}
