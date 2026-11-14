export type Server = {
  id: string;
  name: string;
  hostname: string;
  environment: string;
  description: string | null;
  status: "ONLINE" | "OFFLINE" | "UNKNOWN";
  agentId: string;
  lastSeenAt: string | null;
};

export async function fetchServers(accessToken: string): Promise<Server[]> {
  return request("/api/v1/servers", {
    headers: { Authorization: `Bearer ${accessToken}` }
  });
}
import { request } from "./client";

