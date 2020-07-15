package org.rdkit.neo4j.handlers;

/*-
 * #%L
 * RDKit-Neo4j
 * %%
 * Copyright (C) 2019 RDKit
 * %%
 * Copyright (C) 2019 Evgeny Sorokin
 * @@ All Rights Reserved @@
 * This file is part of the RDKit Neo4J integration.
 * The contents are covered by the terms of the BSD license
 * which is included in the file LICENSE, found at the root
 * of the neo4j-rdkit source tree.
 * #L%
 */

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.kernel.extension.ExtensionType;
import org.neo4j.kernel.extension.KernelExtensionFactory;
import org.neo4j.kernel.impl.spi.KernelContext;
import org.neo4j.kernel.lifecycle.Lifecycle;
import org.neo4j.kernel.lifecycle.LifecycleAdapter;
import org.neo4j.logging.Log;
import org.neo4j.logging.internal.LogService;
import org.rdkit.neo4j.bin.LibraryLoader;
import org.rdkit.neo4j.bin.LoaderException;
import org.rdkit.neo4j.config.RDKitSettings;
import org.rdkit.neo4j.handlers.RDKitEventHandlerExtensionFactory.Dependencies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class enables neo4j kernel to load custom event handler and loads native libraries
 */
public class RDKitEventHandlerExtensionFactory extends KernelExtensionFactory<Dependencies> {
  private static final Logger logger = LoggerFactory.getLogger(RDKitEventHandlerExtensionFactory.class);

  /*
   * Load native libraries here as this factory is retrieved first
   * todo: what if libraries are not loaded?
   */
  static {
    try {
      LibraryLoader.loadLibraries();
    } catch (LoaderException e) {
      logger.error("Unable to load native libraries: RDKit");
      e.printStackTrace();
    }
  }

  @Override
  public Lifecycle newInstance(KernelContext kernelContext, final Dependencies dependencies) {
    return new LifecycleAdapter() {
      final Log log = dependencies.log().getUserLog(RDKitEventHandlerExtensionFactory.class);

      private RDKitEventHandler handler;

      @Override
      public void start() {

        log.info("Starting RDKit trigger watcher");
        boolean sanitize = dependencies.config().get(RDKitSettings.indexSanitize);
        logger.debug("sanitize = %s", sanitize);
        handler = new RDKitEventHandler(dependencies.getGraphDatabaseService(), sanitize);
        dependencies.getGraphDatabaseService().registerTransactionEventHandler(handler);
      }

      @Override
      public void shutdown() {
        log.info("Stopping RDKit trigger watcher");
        if (handler != null)
          dependencies.getGraphDatabaseService().unregisterTransactionEventHandler(handler);
      }
    };
  }

  interface Dependencies {
    GraphDatabaseService getGraphDatabaseService();
    LogService log();
    Config config();
  }

  public RDKitEventHandlerExtensionFactory() {
    super(ExtensionType.DATABASE, "rdkitEventHandler");
  }
}
