/*
 * Copyright 2016-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.mcast.cli;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onlab.packet.IpAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.mcast.api.McastRoute;
import org.onosproject.mcast.api.MulticastRouteService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.HostId;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Displays the source, multicast group flows entries.
 */
@Command(scope = "onos", name = "mcast-host-show", description = "Displays the source, multicast group flows")
public class McastShowHostCommand extends AbstractShellCommand {

    // Format for group line
    private static final String FORMAT_MAPPING = "origin=%s, group=%s, source IP=%s, sources=%s, sinks=%s";

    @Option(name = "-gAddr", aliases = "--groupAddress",
            description = "IP Address of the multicast group",
            valueToShowInHelp = "224.0.0.0",
            required = false, multiValued = false)
    String gAddr = null;

    @Override
    protected void execute() {
        // Get the service
        MulticastRouteService mcastService = get(MulticastRouteService.class);
        // Get the routes
        Set<McastRoute> routes = mcastService.getRoutes();
        // Verify mcast group
        if (!isNullOrEmpty(gAddr)) {
            // Let's find the group
            IpAddress mcastGroup = IpAddress.valueOf(gAddr);
            McastRoute mcastRoute = routes.stream()
                    .filter(route -> route.group().equals(mcastGroup))
                    .findAny().orElse(null);
            // If it exists
            if (mcastRoute != null) {
                printRoute(mcastService, mcastRoute);
            }
            return;
        }
        // Filter ipv4
        Set<McastRoute> ipv4Routes = routes.stream()
                .filter(mcastRoute -> mcastRoute.group().isIp4())
                .collect(Collectors.toSet());
        // Print ipv4 first
        ipv4Routes.stream()
                .sorted(Comparator.comparing(McastRoute::group))
                .forEach(route -> {
                    printRoute(mcastService, route);
                });
        // Filter ipv6
        Set<McastRoute> ipv6Routes = routes.stream()
                .filter(mcastRoute -> mcastRoute.group().isIp6())
                .collect(Collectors.toSet());
        // Then print ipv6
        ipv6Routes.stream()
                .sorted(Comparator.comparing(McastRoute::group))
                .forEach(route -> {
                    printRoute(mcastService, route);
                });
    }

    private void printRoute(MulticastRouteService mcastService, McastRoute route) {
        Map<HostId, Set<ConnectPoint>> sinks = mcastService.routeData(route).sinks();
        Map<HostId, Set<ConnectPoint>> sources = mcastService.routeData(route).sources();
        String srcIp = "*";
        if (route.source().isPresent()) {
            srcIp = route.source().get().toString();
        }

        print(FORMAT_MAPPING, route.type(), route.group(), srcIp, sources, sinks);
    }

}
